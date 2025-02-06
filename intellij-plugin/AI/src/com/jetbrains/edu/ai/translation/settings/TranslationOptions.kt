package com.jetbrains.edu.ai.translation.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.*
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.settings.AIOptionsProvider
import com.jetbrains.edu.ai.translation.ui.TranslationLanguageComboBoxModel
import com.jetbrains.educational.core.format.enum.TranslationLanguage

class TranslationOptions : BoundConfigurable(EduAIBundle.message("settings.ai.translation.display.name")), AIOptionsProvider {
  private val settings = TranslationSettings.getInstance()
  private var autoTranslate: Boolean = settings.autoTranslate
  private var preferableLanguage: TranslationLanguage? = settings.preferableLanguage
  private lateinit var checkBox: Cell<JBCheckBox>

  override fun createPanel(): DialogPanel = panel {
    group(displayName) {
      row(EduAIBundle.message("settings.ai.translation.preferred.language")) {
        comboBox(TranslationLanguageComboBoxModel())
          .bindItem(::preferableLanguage.toNullableProperty())
          .onChanged {
            checkBox.enabled(it.selectedItem != null)
          }
      }
      row {
        checkBox = checkBox(EduAIBundle.message("settings.ai.auto.translate"))
          .enabled(preferableLanguage != null)
          .bindSelected(::autoTranslate)
      }
    }
  }

  override fun apply() {
    val language = preferableLanguage ?: return
    super.apply()
    val autoTranslationProperties = AutoTranslationProperties(language, autoTranslate)
    TranslationSettings.getInstance().setAutoTranslationProperties(autoTranslationProperties)
  }
}