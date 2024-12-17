package com.jetbrains.edu.aiDebugging.core.session

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.util.messages.MessageBusConnection
import com.intellij.xdebugger.*
import com.jetbrains.edu.aiDebugging.core.breakpoint.AIBreakPointService
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.CheckUtils.createTests
import com.jetbrains.edu.learning.checker.CheckUtils.deleteTests
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.getEditor
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.ai.debugger.prompt.core.FixCodeForTestAssistant
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.description.TaskDescription
import com.jetbrains.educational.ml.ai.debugger.prompt.responses.FixCodeForTestResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.jetbrains.edu.aiDebugging.core.ui.AIBreakpointHint


@Service(Service.Level.PROJECT)
class AIDebugSessionService(private val project: Project, private val coroutineScope: CoroutineScope) {

  private var breakpointHint: AIBreakpointHint? = null
  private var connection: MessageBusConnection? = null

  fun runDebuggingSession(task: Task, description: TaskDescription, virtualFiles: List<VirtualFile>, testResult: CheckResult, closeAIDebuggingHint: () -> Unit) {
    coroutineScope.launch {
      withModalProgress(project, EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session")) {
        FixCodeForTestAssistant.getCodeFix(
          description,
          virtualFiles.toNumberedLineMap(),
          testResult.details ?: testResult.message
        )
      }.onSuccess { fixes ->
        val language = project.course?.languageById ?: error("Language is not found")
        val settings = getRunSettingsForFailedTest(task, testResult)
        fixes.forEach {
          val virtualFile = virtualFiles.firstOrNull { file -> file.name == it.fileName } ?: error("Virtual file is not found")
          project.getService(AIBreakPointService::class.java).toggleLineBreakpoint(language, virtualFile, it.wrongCodeLineNumber)
        }
        runWithTests(task, closeAIDebuggingHint) {
          startDebugSession(settings)
        }
        subscribeToDebuggerEvents(task, closeAIDebuggingHint, fixes)
      }.onFailure {
        EduNotificationManager.showErrorNotification(
          project,
          content = EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session.fail")
        )
      }
    }
  }

  private fun List<VirtualFile>.toNumberedLineMap() = runReadAction {
    associate { it.name to it.document.text.lines().mapIndexed { index, line -> "$index: $line" }.joinToString(System.lineSeparator()) }
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
    connection?.disconnect()
    // TODO: make breakpoints regular
  }

  private fun subscribeToDebuggerEvents(task: Task, closeAIDebuggingHint: () -> Unit, fixes: FixCodeForTestResponse) {
    connection = project.messageBus.connect()
    connection?.subscribe(XDebuggerManager.TOPIC, object : XDebuggerManagerListener {
      override fun processStopped(debugProcess: XDebugProcess) {
        debugStopped(task, closeAIDebuggingHint)
      }

      override fun processStarted(debugProcess: XDebugProcess) {
        super.processStarted(debugProcess)
        subscribeToSessionEvents(debugProcess.session, fixes)
      }
    })
  }

  private fun subscribeToSessionEvents(session: XDebugSession, fixes: FixCodeForTestResponse) {
    session.addSessionListener(object : XDebugSessionListener {
      override fun sessionPaused() {
        super.sessionPaused()
        val position = session.currentPosition ?: return
        val editor = position.file.getEditor(project) ?: return
        val message = fixes.firstOrNull {
          it.fileName == position.file.name && it.wrongCodeLineNumber == position.line
        }?.breakpointHint ?: return
        breakpointHint?.close()
        breakpointHint = AIBreakpointHint(message, editor, getTextStartOffset(editor, position.line))
      }

      override fun sessionResumed() {
        super.sessionResumed()
        breakpointHint?.close()
      }
    })
  }

  private fun getTextStartOffset(editor: Editor, line: Int): Int {
    val document = editor.document
    val lineStartOffset = document.getLineStartOffset(line)
    val lineText = document.getText(TextRange(lineStartOffset, document.getLineEndOffset(line)))
    return lineStartOffset + (lineText.indexOfFirst { !it.isWhitespace() }.takeIf { it != -1 } ?: 0)
  }

  private fun Task.getInvisibleTestFiles() = taskFiles.values.filter {
    EduUtilsKt.isTestsFile(this, it.name) && !it.isVisible
  }

  private fun getRunSettingsForFailedTest(task: Task, testResult: CheckResult): RunnerAndConfigurationSettings {
    val methodName = testResult.executedTestsInfo.firstFailed()?.name?.replace(":", ".") ?: error("Method name is not found")
    val settings = runReadAction { task.getAllTestDirectories(project).firstNotNullOfOrNull {
                       ConfigurationContext(it).configurationsFromContext?.firstOrNull()?.configurationSettings
    } } ?: error("No configuration is found")

    val configuration = settings.configuration
    if (configuration is ExternalSystemRunConfiguration) {
      val settingsData = configuration.settings
      settingsData.taskNames = settingsData.taskNames + listOf("--tests", "\"$methodName\"")
    }
    return settings
  }

  private fun startDebugSession(settings: RunnerAndConfigurationSettings) = runInEdt {
    val environment = ExecutionEnvironmentBuilder.create(DefaultDebugExecutor.getDebugExecutorInstance(), settings).activeTarget().build()
    ProgramRunner.getRunner(DefaultDebugExecutor.EXECUTOR_ID, settings.configuration)?.execute(environment)
  }
}
