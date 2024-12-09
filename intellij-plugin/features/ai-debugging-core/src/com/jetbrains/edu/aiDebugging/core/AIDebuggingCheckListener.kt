package com.jetbrains.edu.aiDebugging.core

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import com.jetbrains.edu.aiDebugging.core.session.AIDebugSessionService
import com.jetbrains.edu.aiDebugging.core.ui.AIDebuggingHintInlineBanner
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.description.TaskDescription
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.description.TaskDescriptionType

class AIDebuggingCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (!isAvailable(task, result)) return
    val testDescription = (result.details ?: result.message) + (result.diff ?: "")
    val textToShow = EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.text")

    val aiDebuggingHintBanner = AIDebuggingHintInlineBanner(textToShow).apply {
      addAction(EduAIDebuggingCoreBundle.message("action.Educational.AiDebuggingNotification.start.debugging.session")) {
        showDebugNotification(task, result) { this.close() }
      }
    }
    TaskToolWindowView.getInstance(project).addInlineBannerToCheckPanel(aiDebuggingHintBanner)
  }

  // TODO replace testDescription to some data class with test information
  private fun showDebugNotification(task: Task, testResult: CheckResult, closeAIDebuggingHint: () -> Unit) {
    val project = task.project ?: error("Project is missing")
    val virtualFiles = task.taskFiles.values.filter { it.isVisible }.mapNotNull { it.getVirtualFile(project) }
    if (virtualFiles.isEmpty()) return
    val taskDescription = task.getTaskDescription(project)
    val service = project.service<AIDebugSessionService>()
    service.runDebuggingSession(task, taskDescription, virtualFiles, testResult, closeAIDebuggingHint)
  }

  // TODO: when should we show this button?
  private fun isAvailable(task: Task, result: CheckResult) =
    task.course.courseMode == CourseMode.STUDENT &&
    task.status == CheckStatus.Failed &&
    task is EduTask &&
    result.executedTestsInfo.firstFailed() != null

  private fun Task.getTaskDescription(project: Project): TaskDescription {
    val description = runReadAction { getDescriptionFile(project)?.readText() } ?: error("There are no description for the task")
    return TaskDescription(description, descriptionFormat.toTaskDescriptionType())
  }

  private fun DescriptionFormat.toTaskDescriptionType() =
    when (this) {
      DescriptionFormat.MD -> TaskDescriptionType.MD
      DescriptionFormat.HTML -> TaskDescriptionType.HTML
    }
}
