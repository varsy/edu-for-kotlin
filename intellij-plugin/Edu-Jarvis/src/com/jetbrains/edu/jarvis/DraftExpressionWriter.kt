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
  fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String)

  fun getCodeLineOffset(): Int

  companion object {
    private val EP_NAME = LanguageExtension<DraftExpressionWriter>("Educational.draftExpressionWriter")

    fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String, language: Language) {
      ThreadingAssertions.assertReadAccess()
      EP_NAME.forLanguage(language)?.addDraftExpression(project, element, generatedCode)
             ?: error("Not supported to provide a draft expression for the ${language.displayName} language")
    }

    /**
     * Returns the number of lines before the start of the generated code in the draft expression.
     */
    fun getCodeLineOffset(language: Language): Int
      = EP_NAME.forLanguage(language)?.getCodeLineOffset()
      ?: error("Not supported to provide a draft expression for the ${language.displayName} language")

  }
}
