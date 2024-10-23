package com.jetbrains.edu.learning.hints

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.ThreadingAssertions.assertReadAccess
import com.jetbrains.edu.learning.hints.context.FunctionsToStrings

interface StringExtractor {

  fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings

  companion object {
    private val EP_NAME = LanguageExtension<StringExtractor>("Educational.stringExtractor")

    fun getFunctionsToStringsMap(psiFile: PsiFile, language: Language): FunctionsToStrings {
      assertReadAccess()
      return EP_NAME.forLanguage(language)?.getFunctionsToStringsMap(psiFile) ?: FunctionsToStrings(emptyMap())
    }
  }
}
