package com.jetbrains.edu.aiDebugging.core.session

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.xdebugger.*
import com.jetbrains.edu.aiDebugging.core.breakpoint.AIBreakPointService
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.CheckUtils.createTests
import com.jetbrains.edu.learning.checker.CheckUtils.deleteTests
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.ai.debugger.prompt.core.FixCodeForTestAssistant
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.description.TaskDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class AIDebugSessionService(private val project: Project, private val coroutineScope: CoroutineScope) {

  fun runDebuggingSession(task: Task, description: TaskDescription, virtualFiles: List<VirtualFile>, testResult: CheckResult, closeAIDebuggingHint: () -> Unit) {
    coroutineScope.launch {
      withModalProgress(project, EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session")) {
        FixCodeForTestAssistant.getCodeFix(
          description,
          virtualFiles.first().readText(),
          testResult.details ?: testResult.message
        ) // TODO change to several files and grab text in a proper way
      }.onSuccess { fixes ->
        val language = project.course?.languageById ?: error("Language is not found")
        runReadAction {
          val virtualFile = virtualFiles.first() // TODO change to several files
          val document = virtualFile.document
          fixes.forEach {
            val offset = virtualFile.readText().indexOf(it.wrongCode.split(System.lineSeparator()).firstOrNull() ?: "") // TODO: fix prompt!!
            require(offset >= 0)
            { "There are no offset in the file for the current wrong code: `${it.wrongCode}`" }
            val line = document.getLineNumber(offset)
            project.getService(AIBreakPointService::class.java).toggleLineBreakpoint(language, virtualFile, line)
          }
        }
        runWithTests(task, closeAIDebuggingHint) {
          startDebugSession(task, testResult, closeAIDebuggingHint)
        }
      }.onFailure {
        EduNotificationManager.showErrorNotification(
          project,
          content = EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session.fail")
        )
      }
    }
  }

  private fun runWithTests(task: Task, closeAIDebuggingHint: () -> Unit, execution: () -> Unit) {
    createTests(task.getInvisibleTestFiles(), project)
    try {
      execution()
    } catch (_: Throwable) {
      debugStopped(task, closeAIDebuggingHint)
    }
  }

  private fun debugStopped(task: Task, closeAIDebuggingHint: () -> Unit) {
    deleteTests(task.getInvisibleTestFiles(), project)
    closeAIDebuggingHint()
    // TODO: make breakpoints regular
  }

  private fun subscribeToDebuggerEvents(task: Task, closeAIDebuggingHint: () -> Unit) {
    project.messageBus.connect().subscribe(XDebuggerManager.TOPIC, object : XDebuggerManagerListener {
      override fun processStopped(debugProcess: XDebugProcess) {
        debugStopped(task, closeAIDebuggingHint)
      }
    })
  }

  private fun Task.getInvisibleTestFiles() = taskFiles.values.filter {
    EduUtilsKt.isTestsFile(this, it.name) && !it.isVisible
  }

  private fun startDebugSession(task: Task, testResult: CheckResult, closeAIDebuggingHint: () -> Unit) = runInEdt {
    val methodName = testResult.executedTestsInfo.firstFailed()?.name?.replace(":", ".") ?: error("Method name is not found")
    val settings = task.getAllTestDirectories(project)
                     .firstNotNullOfOrNull { ConfigurationContext(it).configurationsFromContext?.firstOrNull()?.configurationSettings }
                   ?: error("No configuration is found")

    val configuration = settings.configuration
    if (configuration is ExternalSystemRunConfiguration) {
      val settingsData = configuration.settings
      settingsData.taskNames = settingsData.taskNames + listOf("--tests", "\"$methodName\"")
    }
    val environment = ExecutionEnvironmentBuilder.create(DefaultDebugExecutor.getDebugExecutorInstance(), settings).activeTarget().build()
    val runner = ProgramRunner.getRunner(DefaultDebugExecutor.EXECUTOR_ID, settings.configuration)
    runner?.execute(environment)
    subscribeToDebuggerEvents(task, closeAIDebuggingHint)
  }
}
