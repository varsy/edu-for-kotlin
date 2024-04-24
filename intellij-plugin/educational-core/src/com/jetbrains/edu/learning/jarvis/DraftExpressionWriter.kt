package com.jetbrains.edu.learning.jarvis

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

interface DraftExpressionWriter {
  fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String)

  companion object {
    private val EP_NAME = LanguageExtension<DraftExpressionWriter>("Educational.draftExpressionWriter")

    fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String, language: Language) {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      EP_NAME.forLanguage(language)?.addDraftExpression(project, element, generatedCode)
             ?: error("Not supported to provide a draft expression for the ${language.displayName} language")
    }
  }
}