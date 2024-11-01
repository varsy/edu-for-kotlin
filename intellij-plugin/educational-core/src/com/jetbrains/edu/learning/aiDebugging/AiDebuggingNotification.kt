package com.jetbrains.edu.learning.aiDebugging

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.selectedTaskFile
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

object AiDebuggingNotification {

  private var aiDebuggingNotificationPanel: JComponent? = null

  fun addAiDebuggingNotification(task: Task, actionTargetParent: JPanel?, testDescription: String?) {
    if (isAvailable(task)) {
      val textToShow = EduCoreBundle.message("action.Educational.AiDebuggingNotification.text")
      val action = showDebugNotification(task, testDescription)
      val close: () -> Unit = {
        aiDebuggingNotificationPanel?.let {
          actionTargetParent?.remove(aiDebuggingNotificationPanel)
          actionTargetParent?.revalidate()
          actionTargetParent?.repaint()
        }
      }

      val aiDebuggingNotification =
        AiDebuggingNotificationFrame(textToShow, action, actionTargetParent, close)

      aiDebuggingNotificationPanel = aiDebuggingNotification.rootPane
      aiDebuggingNotificationPanel?.let { actionTargetParent?.add(it, BorderLayout.NORTH) }
    }
  }

  @Suppress("DialogTitleCapitalization")
  private fun showDebugNotification(task: Task, testDescription: String?) =
    object : AnAction(EduCoreBundle.message("action.Educational.AiDebuggingNotification.start.debugging.session")) {

      override fun actionPerformed(p0: AnActionEvent) {
        val project = task.project ?: error("Project is missing")
        val code = project.selectedTaskFile?.contents?.textualRepresentation ?: error("There are no task file with code to use")
        val description = task.getDescriptionFile(project)?.readText() ?: error("There are no description for the task")
        //TODO add modality progress
      }
    }

  private fun isAvailable(task: Task) =
    task.course.courseMode == CourseMode.STUDENT && task.status == CheckStatus.Failed // TODO: when should we show this button?
}


