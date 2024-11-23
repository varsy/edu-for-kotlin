package com.jetbrains.edu.agreement

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.submissions.UserAgreementState

class ResetUserAgreementSettings : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    userAgreementSettings().setUserAgreementSettings(
      UserAgreementSettings.UserAgreementProperties(
        UserAgreementState.NOT_SHOWN
      )
    )
    EduNotificationManager.showInfoNotification(
      e.project,
      EduCoreBundle.message("action.Educational.ResetUserAgreementSettings.notification.title"),
      EduCoreBundle.message("action.Educational.ResetUserAgreementSettings.notification.text")
    )
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}