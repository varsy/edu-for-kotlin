package com.jetbrains.edu.cognifire.inspection

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

interface InspectionProcessor {
  fun applyInspections(promptToCodeTranslation: PromptToCodeContent, project: Project, functionSignature: String): PromptToCodeContent

  companion object {
    private val EP_NAME = LanguageExtension<InspectionProcessor>("Educational.inspectionProcessor")

    fun applyInspections(promptToCodeTranslation: PromptToCodeContent, functionSignature: String, project: Project, language: Language) =
      EP_NAME.forLanguage(language)?.applyInspections(promptToCodeTranslation, project, functionSignature)
  }
}
