package com.jetbrains.edu.cognifire.writers

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.cognifire.models.PromptExpression

/**
 * Overwrites the generated prompt into the text of the `prompt` DSL element.
 */
interface PromptExpressionWriter : ExpressionWriter<PromptExpression> {
  override fun addExpression(project: Project, element: PsiElement, text: String, oldExpression: PromptExpression?): PromptExpression?

  companion object {
    private val EP_NAME = LanguageExtension<PromptExpressionWriter>("Educational.promptExpressionWriter")

    fun addPromptExpression(project: Project, element: PsiElement, generatedPrompt: String, oldPromptExpression: PromptExpression, language: Language): PromptExpression {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.addExpression(project, element, generatedPrompt, oldPromptExpression)
             ?: error("Not supported to provide a code expression for the ${language.displayName} language")
    }
  }
}