package com.jetbrains.edu.ai.hints.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.actions.EduActionUtils.performAction
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.ui.isDefault
import org.jetbrains.annotations.NonNls
import javax.swing.JButton
import javax.swing.JComponent

class AcceptHint : ApplyCodeAction() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isGetHintDiff()
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent = JButton(presentation.text).apply {
    isDefault = true
    isFocusable = true
    addActionListener {
      performAction(this@AcceptHint, this, place, presentation)
    }
  }

  override fun showSuccessfulNotification(project: Project) = EduNotificationManager.showInfoNotification(
    project,
    @Suppress("DialogTitleCapitalization") EduAIBundle.message("action.Educational.Hints.AcceptHint.notification.success.title"),
    EduAIBundle.message("action.Educational.Hints.AcceptHint.notification.success.text"),
  )

  override fun showFailedNotification(project: Project) = EduNotificationManager.showErrorNotification(
    project,
    @Suppress("DialogTitleCapitalization") EduAIBundle.message("action.Educational.Hints.AcceptHint.notification.failed.title"),
    EduAIBundle.message("action.Educational.Hints.AcceptHint.notification.failed.text")
  )

  override fun getConfirmationFromDialog(project: Project): Boolean = true

  override val actionId: String = ACTION_ID

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Hints.AcceptHint"
  }
}