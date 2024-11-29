package com.jetbrains.edu.cognifire.parsers

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.cognifire.models.PromptExpression

/**
 * Parses a `prompt` DSL element and returns a [PromptExpression] object if successful.
 */
interface PromptExpressionParser : ExpressionParser<PromptExpression> {
  override fun getExpression(element: PsiElement): PromptExpression?

  companion object {
    private val EP_NAME = LanguageExtension<PromptExpressionParser>("Educational.promptExpressionParser")

    fun parsePromptExpression(promptExpression: PsiElement, language: Language): PromptExpression? {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.getExpression(promptExpression)
    }
  }
}
