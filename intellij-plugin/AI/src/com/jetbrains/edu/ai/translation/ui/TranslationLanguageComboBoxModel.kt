package com.jetbrains.edu.ai.translation.ui

import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import javax.swing.DefaultComboBoxModel

class TranslationLanguageComboBoxModel(private val courseLanguageCode: String? = null) : DefaultComboBoxModel<TranslationLanguage>() {
  init {
    @OptIn(ExperimentalStdlibApi::class)
    val languages = TranslationLanguage.entries
      .filter { it.code != courseLanguageCode }
      .sortedBy { it.label }
    addAll(languages)
  }

  override fun setSelectedItem(anObject: Any?) {
    val language = anObject as? TranslationLanguage
    val objectToSelect = if (language == null || language.code == courseLanguageCode) {
      EduAIBundle.message("ai.translation.choose.language")
    }
    else {
      anObject
    }
    super.setSelectedItem(objectToSelect)
  }
}