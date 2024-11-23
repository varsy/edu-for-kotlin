package com.jetbrains.edu.learning.checker.details

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import javax.swing.JComponent

abstract class CheckDetailsView {

  abstract fun showOutput(message: String)

  abstract fun showCheckResultDetails(title: String, message: String)

  abstract fun showResult(title: String, panel: JComponent)

  abstract fun clear()

  companion object {
    fun getInstance(project: Project): CheckDetailsView {
      if (!project.isEduProject()) {
        error("Attempt to get CheckDetailsViewImpl for non-edu project")
      }
      if (!userAgreementSettings().isPluginAllowed) {
        error("Attempt to get CheckDetailsViewImpl when agreement not accepted")
      }
      return project.service()
    }
  }
}