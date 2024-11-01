package com.jetbrains.edu.aiHints.core

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface FunctionDiffReducer {
  fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement?

  companion object {
    private val EP_NAME = LanguageExtension<FunctionDiffReducer>("aiHints.functionDiffReducer")

    @RequiresReadLock
    fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project, language: Language): PsiElement? {
      val functionDiffReducer = EP_NAME.forLanguage(language) ?: error("${EP_NAME.name} is not implemented for $language")
      return functionDiffReducer.reduceDiffFunctions(function, modifiedFunction, project)
    }
  }
}