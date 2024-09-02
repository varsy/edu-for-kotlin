package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.performAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.ui.isDefault
import org.jetbrains.annotations.NonNls
import javax.swing.JButton
import javax.swing.JComponent

class AcceptHintAction : ApplyCodeActionBase() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isNextStepHintDiff()
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent = JButton(presentation.text).apply {
    isDefault = true
    isFocusable = true
    addActionListener {
      performAction(this@AcceptHintAction, this, place, presentation)
    }
  }

  override fun showSuccessfulNotification(project: Project) = EduNotificationManager.showInfoNotification(
    project,
    @Suppress("DialogTitleCapitalization") EduCoreBundle.message("action.Educational.Assistant.AcceptHint.notification.success.title"),
    EduCoreBundle.message("action.Educational.Assistant.AcceptHint.notification.success.text"),
  )

  override fun showFailedNotification(project: Project) = EduNotificationManager.showErrorNotification(
    project,
    @Suppress("DialogTitleCapitalization") EduCoreBundle.message("action.Educational.Assistant.AcceptHint.notification.failed.title"),
    EduCoreBundle.message("action.Educational.Assistant.AcceptHint.notification.failed.text")
  )

  override fun getConfirmationFromDialog(project: Project): Boolean = true

  override val actionId: String = ACTION_ID

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Assistant.AcceptHint"
  }
}
