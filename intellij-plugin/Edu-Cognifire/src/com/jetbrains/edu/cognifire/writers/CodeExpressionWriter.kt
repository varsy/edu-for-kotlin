package com.jetbrains.edu.cognifire.writers

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.cognifire.models.CodeExpression

/**
 * Adds a `code` DSL block with the generated code to the code after the given [PsiElement].
 */
interface CodeExpressionWriter : ExpressionWriter<CodeExpression> {
  override fun addExpression(project: Project, element: PsiElement, text: String, oldExpression: CodeExpression?): CodeExpression?

  companion object {
    private val EP_NAME = LanguageExtension<CodeExpressionWriter>("Educational.codeExpressionWriter")

    /**
     * Adds a code expression with the generated code to the code after the given [PsiElement].
     * @return the offset of the code inside the code block
     * @throws IllegalStateException if the language doesn't support providing a code expression
     */
    fun addCodeExpression(project: Project, element: PsiElement, generatedCode: String, language: Language): CodeExpression {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.addExpression(project, element, generatedCode)
             ?: error("Not supported to provide a code expression for the ${language.displayName} language")
    }

  }
}
