package com.jetbrains.edu.agreement

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class ShowUserAgreementDialog : DumbAwareAction("Show User Agreement Dialog") {
  override fun actionPerformed(e: AnActionEvent) {
    UserAgreementDialog.showUserAgreementDialog(e.project)
  }
}