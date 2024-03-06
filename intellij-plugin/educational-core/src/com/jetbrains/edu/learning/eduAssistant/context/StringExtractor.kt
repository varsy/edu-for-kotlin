package com.jetbrains.edu.learning.eduAssistant.context

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignature

interface StringExtractor {
  // If the function does not contain any strings we store an empty list
  fun getFunctionsToStringsMap(psiFile: PsiFile): Map<FunctionSignature, List<String>>

  companion object {
    private val EP_NAME = LanguageExtension<StringExtractor>("Educational.stringExtractor")

    fun getFunctionsToStringsMap(psiFile: PsiFile, language: Language): Map<FunctionSignature, List<String>> {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.getFunctionsToStringsMap(psiFile) ?: emptyMap()
    }
  }
}
