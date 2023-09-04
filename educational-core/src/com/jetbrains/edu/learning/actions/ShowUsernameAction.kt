package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBAccountInfoService

class ShowUsernameAction : DumbAwareAction("Show user name") {
  override fun actionPerformed(e: AnActionEvent) {
    val res = JBAccountInfoService.getInstance()?.userData
    if (res == null) {
      Messages.showMessageDialog(
        "UserData is null, service instance: " + JBAccountInfoService.getInstance(),
        "Debug",
        Messages.getInformationIcon()
      )
    } else {
      Messages.showMessageDialog(
        "Current logged in user: " + res.loginName + ", " + res.id,
        "User Info",
        Messages.getInformationIcon()
      )
    }
  }
}