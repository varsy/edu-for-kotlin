package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.jvm.MainFileProvider
import com.jetbrains.edu.jvm.messages.EduJVMBundle
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.checker.CheckUtils.COMPILATION_FAILED_MESSAGE
import com.jetbrains.edu.learning.checker.CheckUtils.postProcessOutput
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.checker.TestsOutputParser.Companion.STUDY_PREFIX
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.ext.dirName
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.gradleSanitizeName
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_WIN
import com.jetbrains.edu.learning.messages.EduFormatBundle
import org.jetbrains.plugins.gradle.settings.GradleSettings
import org.jetbrains.plugins.gradle.settings.TestRunner

const val MAIN_CLASS_PROPERTY_PREFIX = "-PmainClass="

// TODO: consider to use init scripts (https://docs.gradle.org/current/userguide/init_scripts.html) for customization
// Should be passed to gradle command to add `#educational_plugin` prefix for `run` task output
const val EDUCATIONAL_RUN_PROPERTY = "-PeducationalRun=true"

const val UTF_8_ENCODING_PARAM = "-Dfile.encoding=UTF-8"

const val JAVA_HOME = "JAVA_HOME"

const val JAVA_OPTS = "JAVA_OPTS"

const val CHECKER_VERSION = "#educational_plugin_checker_version "

const val TEST_TASK_NAME = "test"

fun getGradleProjectName(task: Task) =
  if (task.lesson.section != null)
    "${gradleSanitizeName(task.lesson.section!!.name)}-${gradleSanitizeName(task.lesson.name)}-${gradleSanitizeName(task.dirName)}"
  else
    "${gradleSanitizeName(task.lesson.name)}-${gradleSanitizeName(task.dirName)}"

class GradleCommandLine private constructor(
  private val cmd: GeneralCommandLine,
  private val taskName: String
) {

  fun launchAndCheck(indicator: ProgressIndicator): CheckResult {
    val output = launch(indicator) ?: return CheckResult.failedToCheck
    if (!output.isSuccess) return CheckResult(CheckStatus.Failed, output.firstMessage.xmlEscaped, output.messages.joinToString("\n"))

    // TODO: do not use `TestsOutputParser` here
    return TestsOutputParser().getCheckResult(output.messages.map { STUDY_PREFIX + it }, needEscapeResult = true)
  }

  fun launch(indicator: ProgressIndicator): GradleOutput? {
    val output = try {
      val handler = CapturingProcessHandler(cmd)
      handler.runProcessWithProgressIndicator(indicator)
    }
    catch (e: ExecutionException) {
      LOG.info("Failed to launch checking", e)
      return null
    }

    val stderr = output.stderr
    if (stderr.isNotEmpty() && output.stdout.isEmpty()) {
      return GradleOutput(false, listOf(stderr))
    }

    //gradle prints compilation failures to error stream
    if (GradleStderrAnalyzer().tryToGetCheckResult(stderr) != null) {
      return GradleOutput(false, listOf(COMPILATION_FAILED_MESSAGE, output.stderr))
    }

    if (!output.stdout.contains(taskName)) {
      LOG.warn("#educational: executing $taskName fails: \n" + output.stdout)
      return GradleOutput(false, listOf(EduFormatBundle.message("error.failed.to.launch.checking"), stderr, output.stdout))
    }

    return GradleOutput(true, collectMessages(output))
  }

  private fun collectMessages(output: ProcessOutput): List<String> {
    val currentMessage = mutableListOf<String>()
    val allMessages = mutableListOf<String>()

    var checkerVersion = 0

    fun addCurrentMessageIfNeeded() {
      if (currentMessage.isNotEmpty()) {
        allMessages += currentMessage.joinToString("")
      }
    }

    for (line in output.stdoutLines) {
      if (line.startsWith(CHECKER_VERSION)) {
        checkerVersion = line.removePrefix(CHECKER_VERSION).toInt()
        continue
      }
      if (line.startsWith(STUDY_PREFIX)) {
        val messageLine = line.removePrefix(STUDY_PREFIX)
        currentMessage.add(computeCurrentMessage(messageLine, checkerVersion))
      }
      else {
        addCurrentMessageIfNeeded()
        currentMessage.clear()
      }
    }

    addCurrentMessageIfNeeded()
    return allMessages
  }

  private fun computeCurrentMessage(messageLine: String, checkerVersion: Int): String {
    if (checkerVersion == 0) return messageLine + "\n"
    return messageLine.ifEmpty { "\n" }
  }

  companion object {

    private val LOG: Logger = Logger.getInstance(GradleCommandLine::class.java)

    fun create(project: Project, command: String, vararg additionalParams: String): GradleCommandLine? {
      val basePath = project.basePath ?: return null
      val projectJdkPath = ProjectRootManager.getInstance(project).projectSdk?.homePath ?: return null
      val projectPath = FileUtil.toSystemDependentName(basePath)
      val javaOpts = calculateJavaOpts()
      val cmd = GeneralCommandLine()
        .withEnvironment(JAVA_HOME, projectJdkPath)
        .withEnvironment(JAVA_OPTS, javaOpts)
        .withWorkDirectory(FileUtil.toSystemDependentName(basePath))
        .withExePath(if (SystemInfo.isWindows) FileUtil.join(projectPath, GRADLE_WRAPPER_WIN) else "./$GRADLE_WRAPPER_UNIX")
        .withParameters(command)
        .withCharset(Charsets.UTF_8)
        .withParameters(*additionalParams)

      return GradleCommandLine(cmd, command)
    }

    private fun calculateJavaOpts() : String {
      val javaOpts = System.getenv(JAVA_OPTS) ?: return UTF_8_ENCODING_PARAM
      return when {
        javaOpts.isEmpty() -> UTF_8_ENCODING_PARAM
        // don't override user's JAVA_OPTS and -Dfile.encoding
        javaOpts.contains("file.encoding") -> javaOpts
        else -> "$javaOpts $UTF_8_ENCODING_PARAM"
      }
    }
  }
}

class GradleOutput(val isSuccess: Boolean, _messages: List<String>) {
  val messages = _messages.map { postProcessOutput(it) }

  val firstMessage: String get() = messages.firstOrNull { it.isNotBlank() } ?: "<no output>"
}

/**
 * Run gradle 'run' task.
 * Returns gradle output if task was successfully executed, otherwise returns CheckResult.
 */
fun runGradleRunTask(project: Project, task: Task, indicator: ProgressIndicator): Result<String, CheckResult> {
  val mainClassName = findMainClass(project, task)
                      ?: return CodeExecutor.resultUnchecked(EduJVMBundle.message("error.no.main", task.name))
  val taskName = if (task.hasSeparateModule(project)) ":${getGradleProjectName(task)}:run" else "run"

  val gradleOutput = GradleCommandLine.create(
    project,
    taskName,
    "$MAIN_CLASS_PROPERTY_PREFIX$mainClassName",
    EDUCATIONAL_RUN_PROPERTY
  )
                       ?.launch(indicator)
                     ?: return Err(GradleEnvironmentChecker.getFailedToLaunchCheckingResult(project))

  if (!gradleOutput.isSuccess) {
    return Err(
      CheckResult(CheckStatus.Failed, gradleOutput.firstMessage.xmlEscaped, gradleOutput.messages.joinToString("\n"))
    )
  }

  return Ok(gradleOutput.firstMessage)
}

private fun findMainClass(project: Project, task: Task): String? =
  runReadActionInSmartMode(project) {
    val language = task.course.languageById ?: return@runReadActionInSmartMode null
    val selectedFile = project.selectedVirtualFile
    if (selectedFile != null) {
      val fileTask = selectedFile.getContainingTask(project)
      if (fileTask == task) {
        val mainClass = MainFileProvider.getMainClassName(project, selectedFile, language)
        if (mainClass != null) return@runReadActionInSmartMode mainClass
      }
    }

    for ((_, taskFile) in task.taskFiles) {
      val file = taskFile.getVirtualFile(project) ?: continue
      return@runReadActionInSmartMode MainFileProvider.getMainClassName(project, file, language) ?: continue
    }
    null
  }

/**
 * There are two types of supported gradle projects: module-per-task and one module for the whole course
 */
fun Task.hasSeparateModule(project: Project): Boolean {
  val taskDir = getDir(project.courseDir) ?: error("Dir for task $name not found")
  val taskModule = ModuleUtil.findModuleForFile(taskDir, project) ?: error("Module for task $name not found")
  val courseModule = ModuleUtil.findModuleForFile(project.courseDir, project) ?: error("Module for course not found")
  if (taskModule == courseModule) {
    return false
  }
  return taskModule.name != "${courseModule.name}.test"
}

inline fun <T> withGradleTestRunner(project: Project, task: Task, action: () -> T): T? {
  val taskDir = task.getDir(project.courseDir) ?: return null
  val module = ModuleUtil.findModuleForFile(taskDir, project) ?: return null
  val path = ExternalSystemApiUtil.getExternalRootProjectPath(module) ?: return null
  val settings = GradleSettings.getInstance(project).getLinkedProjectSettings(path) ?: return null

  val oldValue = settings.testRunner
  settings.testRunner = TestRunner.GRADLE

  return try {
    action()
  }
  finally {
    settings.testRunner = oldValue
  }
}
