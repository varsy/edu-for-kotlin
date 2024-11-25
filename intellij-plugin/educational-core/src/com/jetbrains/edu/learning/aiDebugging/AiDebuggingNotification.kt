package com.jetbrains.edu.learning.aiDebugging

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.learning.aiDebugging.session.AIDebugSessionService
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.description.TaskDescription
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.description.TaskDescriptionType
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

object AiDebuggingNotification {

  private var aiDebuggingNotificationPanel: JComponent? = null

  fun addAiDebuggingNotification(task: Task, actionTargetParent: JPanel?, checkResult: CheckResult) {
    if (!isAvailable(task)) return
    val textToShow = EduCoreBundle.message("action.Educational.AiDebuggingNotification.text")
    val testDescription = checkResult.details ?: checkResult.message
    val action = showDebugNotification(task, testDescription)
    val closeCallback: () -> Unit = {
      aiDebuggingNotificationPanel?.let {
        actionTargetParent?.remove(aiDebuggingNotificationPanel)
        actionTargetParent?.revalidate()
        actionTargetParent?.repaint()
      }
    }

    val aiDebuggingNotification =
      AiDebuggingNotificationFrame(textToShow, action, actionTargetParent, closeCallback)

    aiDebuggingNotificationPanel = aiDebuggingNotification.rootPane
    aiDebuggingNotificationPanel?.let { actionTargetParent?.add(it, BorderLayout.NORTH) }
  }

  // TODO replace testDescription to some data class with test information
  @Suppress("DialogTitleCapitalization")
  private fun showDebugNotification(task: Task, testDescription: String) =
    object : AnAction(EduCoreBundle.message("action.Educational.AiDebuggingNotification.start.debugging.session")) {

      override fun actionPerformed(p0: AnActionEvent) {
        val project = task.project ?: error("Project is missing")
        val virtualFiles = listOf(
          project.selectedTaskFile?.getVirtualFile(project)
          ?: error("There are no virtual file for the selected task`")
        ) // TODO take all task files
        val taskDescription = task.getTaskDescription(project)
        val service = project.service<AIDebugSessionService>()
        service.runDebuggingSession(taskDescription, virtualFiles, testDescription)
      }
    }

  private fun isAvailable(task: Task) =
    task.course.courseMode == CourseMode.STUDENT && task.status == CheckStatus.Failed // TODO: when should we show this button?

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


