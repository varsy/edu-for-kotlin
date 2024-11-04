package com.jetbrains.edu.learning.checker

import com.intellij.execution.*
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.impl.ExecutionManagerImpl
import com.intellij.execution.process.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.RunTaskAction
import com.jetbrains.edu.learning.checker.tests.TestResultCollector
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getCodeTaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.runReadActionInSmartMode
import com.jetbrains.edu.learning.toPsiFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object CheckUtils {

  private val LOG: Logger = logger<CheckUtils>()

  val COMPILATION_ERRORS = listOf("Compilation failed", "Compilation error")

  val CONGRATULATIONS: String
    get() = EduFormatBundle.message("check.correct.solution")
  val COMPILATION_FAILED_MESSAGE: String
    get() = EduCoreBundle.message("check.error.compilation.failed")
  val NOT_RUNNABLE_MESSAGE: String
    get() = EduCoreBundle.message("check.error.solution.not.runnable")
  val SYNTAX_ERROR_MESSAGE: String
    get() = EduCoreBundle.message("check.error.syntax.error")
  val EXECUTION_ERROR_MESSAGE: String
    get() = EduCoreBundle.message("check.execution.error")
  val ERRORS: Set<String>
    get() = setOf(
      COMPILATION_FAILED_MESSAGE,
      EduFormatBundle.message("error.failed.to.launch.checking"),
      SYNTAX_ERROR_MESSAGE,
      EXECUTION_ERROR_MESSAGE
    )

  /**
   * Some testing frameworks add attributes to be shown in console (ex. Jest - ANSI color codes)
   * which are not supported in Task Description, so they need to be removed
   */
  fun removeAttributes(text: String): String {
    val buffer = StringBuilder()
    AnsiEscapeDecoder().escapeText(text, ProcessOutputTypes.STDOUT) { chunk, _ ->
      buffer.append(chunk)
    }
    return buffer.toString()
  }

  fun fillWithIncorrect(message: String): String =
    message.nullize(nullizeSpaces = true) ?: EduCoreBundle.message("check.incorrect")

  fun createDefaultRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val taskFile = task.getCodeTaskFile(project) ?: return null
    return runReadAction {
      val psiFile = taskFile.getDocument(project)?.toPsiFile(project) ?: return@runReadAction null
      ConfigurationContext(psiFile).configuration
    }
  }

  private fun getCustomRunConfiguration(project: Project, task: Task, predicate: (VirtualFile) -> Boolean): RunnerAndConfigurationSettings? {
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find directory of `${task.name}` task")
    val runConfigurationDir = taskDir.findChild(EduNames.RUN_CONFIGURATION_DIR) ?: return null
    val runConfigurationFile = runConfigurationDir.children.firstOrNull {
      predicate(it) && it.name.endsWith(".run.xml")
    } ?: return null
    val path = runConfigurationFile.path
    return RunManager.getInstance(project).allSettings.find {
      it.pathIfStoredInArbitraryFileInProject == path
    }
  }

  fun getCustomRunConfigurationForChecker(project: Project, task: Task): RunnerAndConfigurationSettings? =
    getCustomRunConfiguration(project, task) {
      it.name != RunTaskAction.RUN_CONFIGURATION_FILE_NAME
    }

  fun getCustomRunConfigurationForRunner(project: Project, task: Task): RunnerAndConfigurationSettings? =
    getCustomRunConfiguration(project, task) {
      it.name == RunTaskAction.RUN_CONFIGURATION_FILE_NAME
    }

  fun postProcessOutput(output: String): String {
    return output.replace(System.lineSeparator(), "\n")
  }

  fun createRunConfiguration(project: Project, taskFile: VirtualFile?): RunnerAndConfigurationSettings? {
    return runReadActionInSmartMode(project) {
      val item = PsiUtilCore.findFileSystemItem(project, taskFile)
      if (item == null) null else ConfigurationContext(item).configuration
    }
  }

  /**
   * @return true if execution finished successfully, false otherwise
   */
  fun executeRunConfigurations(
    project: Project,
    configurations: List<RunnerAndConfigurationSettings>,
    indicator: ProgressIndicator,
    executionListener: ExecutionListener? = null,
    processListener: ProcessListener? = null,
    testResultCollector: TestResultCollector? = null
  ): Boolean {
    val connection = project.messageBus.connect()
    try {
      testResultCollector?.startCollecting(connection)
      return executeRunConfigurations(connection, configurations, indicator, executionListener, processListener)
    }
    finally {
      connection.disconnect()
    }
  }

  private fun executeRunConfigurations(
    connection: MessageBusConnection,
    configurations: List<RunnerAndConfigurationSettings>,
    indicator: ProgressIndicator,
    executionListener: ExecutionListener?,
    processListener: ProcessListener?,
  ): Boolean {
    if (configurations.isEmpty()) return true

    val latch = CountDownLatch(configurations.size)
    val context = Context(processListener, executionListener, latch)
    Disposer.register(connection, context)

    var hasBrokenConfiguration = false

    runInEdt {
      connection.subscribe(
        ExecutionManager.EXECUTION_TOPIC,
        CheckExecutionListener(DefaultRunExecutor.EXECUTOR_ID, context)
      )

      for (configuration in configurations) {
        if (hasBrokenConfiguration) {
          latch.countDown()
          continue
        }

        hasBrokenConfiguration = try {
          val startedSuccessfully = configuration.startRunConfigurationExecution(context)
          hasBrokenConfiguration || !startedSuccessfully
        }
        catch (e: Throwable) {
          LOG.warn(e)
          latch.countDown()
          true
        }
      }
    }

    while (!indicator.isCanceled) {
      val result = latch.await(100, TimeUnit.MILLISECONDS)
      if (result) break
    }
    if (indicator.isCanceled) {
      Disposer.dispose(context)
    }
    return !hasBrokenConfiguration
  }

  /**
   * Returns `true` if configuration execution is started successfully, `false` otherwise
   */
  @Throws(ExecutionException::class)
  private fun RunnerAndConfigurationSettings.startRunConfigurationExecution(context: Context): Boolean {
    val runner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, configuration)
    if (runner == null) {
      context.latch.countDown()
      return false
    }

    val env = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(), this).activeTarget().build()
    @Suppress("UnstableApiUsage")
    env.callback = ProgramRunner.Callback { descriptor ->
      // Descriptor can be null in some cases.
      // For example, IntelliJ Rust's test runner provides null here if compilation fails
      if (descriptor == null) {
        context.latch.countDown()
        return@Callback
      }

      Disposer.register(context) {
        ExecutionManagerImpl.stopProcess(descriptor)
      }
      val processHandler = descriptor.processHandler
      if (processHandler != null) {
        processHandler.addProcessListener(object : ProcessAdapter() {
          override fun processTerminated(event: ProcessEvent) {
            context.latch.countDown()
          }
        })
        context.processListener?.let { processHandler.addProcessListener(it) }
      }
    }

    context.environments.add(env)
    runner.execute(env)
    return true
  }

  private class CheckExecutionListener(
    private val executorId: String,
    private val context: Context,
  ) : ExecutionListener {
    override fun processStartScheduled(executorId: String, env: ExecutionEnvironment) {
      checkAndExecute(executorId, env) {
        context.executionListener?.processStartScheduled(executorId, env)
      }
    }

    override fun processNotStarted(executorId: String, env: ExecutionEnvironment) {
      checkAndExecute(executorId, env) {
        context.latch.countDown()
        context.executionListener?.processNotStarted(executorId, env)
      }
    }

    override fun processStarting(executorId: String, env: ExecutionEnvironment) {
      checkAndExecute(executorId, env) {
        context.executionListener?.processStarting(executorId, env)
      }
    }

    override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
      checkAndExecute(executorId, env) {
        context.executionListener?.processStarted(executorId, env, handler)
      }
    }

    override fun processTerminating(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
      checkAndExecute(executorId, env) {
        context.executionListener?.processTerminating(executorId, env, handler)
      }
    }

    override fun processTerminated(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler, exitCode: Int) {
      checkAndExecute(executorId, env) {
        context.executionListener?.processTerminated(executorId, env, handler, exitCode)
      }
    }

    private fun checkAndExecute(executorId: String, env: ExecutionEnvironment, action: () -> Unit) {
      if (this.executorId == executorId && env in context.environments) {
        action()
      }
    }
  }

  private class Context(
    val processListener: ProcessListener?,
    val executionListener: ExecutionListener?,
    val latch: CountDownLatch
  ) : Disposable {

    val environments: MutableList<ExecutionEnvironment> = mutableListOf()

    override fun dispose() {}
  }
}
