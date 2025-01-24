package com.jetbrains.edu.aiHints.python

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.aiHints.core.FunctionDiffReducer

class PyFunctionDiffReducer : FunctionDiffReducer {
  @Suppress("unused")
  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement = modifiedFunction
}