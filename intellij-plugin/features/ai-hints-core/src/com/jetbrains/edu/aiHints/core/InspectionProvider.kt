package com.jetbrains.edu.aiHints.core

import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface InspectionProvider {
  val inspections: Set<String>

  companion object {
    private val EP_NAME = LanguageExtension<InspectionProvider>("aiHints.inspectionProvider")

    @RequiresReadLock
    fun getInspections(language: Language): List<LocalInspectionTool> {
      val inspectionProvider = EP_NAME.forLanguage(language) ?: error("${EP_NAME.name} is not implemented for $language")
      return LocalInspectionEP.LOCAL_INSPECTION.extensions
        .filter { it.language == language.id }
        .mapNotNull { it.instantiateTool().asSafely<LocalInspectionTool>() }
        .filter { it.id in inspectionProvider.inspections }
    }
  }
}