package com.jetbrains.edu.cognifire.parsers

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.cognifire.models.CodeExpression

/**
 * Parses a `code` DSL element and returns a code as a string if successful.
 */
interface CodeExpressionParser : ExpressionParser<CodeExpression> {
  override fun getExpression(element: PsiElement): CodeExpression?

  companion object {
    private val EP_NAME = LanguageExtension<CodeExpressionParser>("Educational.codeExpressionParser")

    fun getCodeExpression(element: PsiElement, language: Language): CodeExpression? {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.getExpression(element)
    }
  }
}
