package com.jetbrains.edu.learning.eduAssistant.context.differ

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

interface FunctionDiffReducer {

  fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement?

  companion object {
    private val EP_NAME = LanguageExtension<FunctionDiffReducer>("Educational.functionDiffReducer")

    fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project, language: Language): PsiElement? {
      return EP_NAME.forLanguage(language)?.reduceDiffFunctions(function, modifiedFunction, project)
    }
  }
}