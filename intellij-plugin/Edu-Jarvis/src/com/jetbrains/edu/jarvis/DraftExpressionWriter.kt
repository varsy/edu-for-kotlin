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

  companion object {
    private val EP_NAME = LanguageExtension<DraftExpressionWriter>("Educational.draftExpressionWriter")

    fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String, language: Language) {
      ThreadingAssertions.assertReadAccess()
      EP_NAME.forLanguage(language)?.addDraftExpression(project, element, generatedCode)
             ?: error("Not supported to provide a draft expression for the ${language.displayName} language")
    }
  }
}
