package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import org.jetbrains.annotations.NonNls
import javax.swing.JButton
import javax.swing.JComponent

class AcceptHintAction : ApplyCodeActionBase() {

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent =
    object : JButton(presentation.text) {
      override fun isDefaultButton(): Boolean = true
      override fun isFocusable(): Boolean = true
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
