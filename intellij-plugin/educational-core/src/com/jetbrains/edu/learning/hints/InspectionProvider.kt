package com.jetbrains.edu.learning.hints

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager

interface InspectionProvider {
  fun getInspections(): List<LocalInspectionTool>

  companion object {
    private val EP_NAME = LanguageExtension<InspectionProvider>("Educational.inspectionProvider")

    fun getInspections(language: Language): List<LocalInspectionTool> {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.getInspections() ?: emptyList()
    }
  }
}
