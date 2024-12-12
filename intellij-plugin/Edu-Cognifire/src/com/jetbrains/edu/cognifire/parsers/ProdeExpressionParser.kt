package com.jetbrains.edu.cognifire.parsers

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.cognifire.models.ProdeExpression

interface ProdeExpressionParser {
  fun getProdeExpressions(file: PsiFile): List<ProdeExpression>

  companion object {
    private val EP_NAME = LanguageExtension<ProdeExpressionParser>("Educational.prodeExpressionParser")

    fun getProdeExpressions(file: PsiFile, language: Language): List<ProdeExpression> {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.getProdeExpressions(file) ?: emptyList()
    }
  }
}
