package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OptionsProvider
import com.jetbrains.edu.learning.submissions.UserAgreementState

class UserAgreementOptions : BoundConfigurable(EduCoreBundle.message("user.agreement.settings.title")), OptionsProvider {
  private val userAgreementSettings: UserAgreementSettings = userAgreementSettings()
  private var pluginAgreement = userAgreementSettings.pluginAgreement

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row {
        @Suppress("DialogTitleCapitalization")
        checkBox(EduCoreBundle.message("user.agreement.settings.checkbox"))
          .bindSelected(::pluginAgreement)
          .enabled(!userAgreementSettings.isNotShown)
      }
    }
  }

  override fun isModified(): Boolean {
    return pluginAgreement != userAgreementSettings.pluginAgreement
  }

  override fun apply() {
    super.apply()
    if (!isModified) {
      return
    }
    val userAgreementState = if (pluginAgreement) UserAgreementState.ACCEPTED else UserAgreementState.TERMINATED
    userAgreementSettings.setUserAgreementSettings(UserAgreementSettings.UserAgreementProperties(userAgreementState))
  }
}