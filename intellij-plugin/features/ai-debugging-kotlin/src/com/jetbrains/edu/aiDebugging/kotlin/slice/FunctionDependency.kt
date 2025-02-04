package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement

typealias PsiElementToDependencies = MutableMap<PsiElement, HashSet<PsiElement>>

abstract class FunctionDependency {
  val dependenciesForward = mutableMapOf<PsiElement, HashSet<PsiElement>>()
  val dependenciesBackward = mutableMapOf<PsiElement, HashSet<PsiElement>>()

  abstract fun PsiElement.addDependency(other: PsiElement)
}
