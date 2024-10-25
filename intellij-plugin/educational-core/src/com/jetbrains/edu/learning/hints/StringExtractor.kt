package com.jetbrains.edu.learning.hints

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.learning.hints.context.FunctionsToStrings

interface StringExtractor {
  fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings

  companion object {
    private val EP_NAME = LanguageExtension<StringExtractor>("Educational.stringExtractor")

    @RequiresReadLock
    fun getFunctionsToStringsMap(psiFile: PsiFile, language: Language): FunctionsToStrings {
      val stringExtractor = EP_NAME.forLanguage(language) ?: error("$EP_NAME is not implemented for $language")
      return stringExtractor.getFunctionsToStringsMap(psiFile)
    }
  }
}