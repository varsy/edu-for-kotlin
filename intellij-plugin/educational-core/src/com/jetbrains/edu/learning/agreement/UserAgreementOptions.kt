package com.jetbrains.edu.learning.agreement

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.learning.settings.OptionsProvider

class UserAgreementOptions : BoundConfigurable("User Agreement"), OptionsProvider {
  private val userAgreementSettings: UserAgreementSettings = userAgreementSettings()
  private var pluginAgreement = userAgreementSettings.pluginAgreement

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row("Entire Plugin User Agreement") {
        checkBox("Agreee").bindSelected(::pluginAgreement)
      }
    }
  }

}