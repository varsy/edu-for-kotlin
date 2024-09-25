package com.jetbrains.edu.kotlin.cognifire.utils

import com.intellij.psi.PsiElement

fun findBlock(
  element: PsiElement,
  step: (PsiElement) -> PsiElement?,
  condition: (PsiElement) -> Boolean
): PsiElement? {
  var possibleBlock: PsiElement? = element
  while(possibleBlock != null && !condition(possibleBlock)) {
    possibleBlock = step(possibleBlock)
  }
  return possibleBlock
}
