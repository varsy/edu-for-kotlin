package com.jetbrains.edu.learning.hints

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface InspectionProvider {
  fun getInspections(): List<LocalInspectionTool>

  companion object {
    private val EP_NAME = LanguageExtension<InspectionProvider>("Educational.inspectionProvider")

    @RequiresReadLock
    fun getInspections(language: Language): List<LocalInspectionTool> {
      val inspectionProvider = EP_NAME.forLanguage(language) ?: error("$EP_NAME is not implemented for $language")
      return inspectionProvider.getInspections()
    }
  }
}