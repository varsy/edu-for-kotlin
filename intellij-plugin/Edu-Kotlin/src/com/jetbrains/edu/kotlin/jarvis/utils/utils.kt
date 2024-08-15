package com.jetbrains.edu.kotlin.jarvis.utils

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker

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


internal fun PsiElement.isDescriptionBlock() = text.startsWith(DESCRIPTION) &&
                                               JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, this.language)

const val RETURN_DRAFT = "return draft"
const val DESCRIPTION = "description"
const val DRAFT = "draft"
