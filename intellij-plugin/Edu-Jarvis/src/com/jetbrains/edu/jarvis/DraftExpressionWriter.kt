package com.jetbrains.edu.jarvis

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * Adds a `draft` DSL block with the generated code to the code after the given [PsiElement].
 */
interface DraftExpressionWriter {
  fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String, returnType: String): Int

  companion object {
    private val EP_NAME = LanguageExtension<DraftExpressionWriter>("Educational.draftExpressionWriter")

    /**
     * Adds a draft expression with the generated code to the code after the given [PsiElement].
     * @return the offset of the code inside the draft block
     * @throws IllegalStateException if the language does not support providing a draft expression
     */
    fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String, returnType: String, language: Language): Int {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.addDraftExpression(project, element, generatedCode, returnType)
             ?: error("Not supported to provide a draft expression for the ${language.displayName} language")
    }

  }
}
