package com.jetbrains.edu.learning.hints

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

interface FunctionDiffReducer {
  fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement?

  companion object {
    private val EP_NAME = LanguageExtension<FunctionDiffReducer>("Educational.functionDiffReducer")

    fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project, language: Language): PsiElement? {
      val functionDiffReducer = EP_NAME.forLanguage(language) ?: error("$EP_NAME is not implemented for $language")
      return functionDiffReducer.reduceDiffFunctions(function, modifiedFunction, project)
    }
  }
}