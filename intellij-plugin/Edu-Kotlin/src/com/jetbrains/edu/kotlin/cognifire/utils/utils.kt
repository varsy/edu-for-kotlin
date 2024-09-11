package com.jetbrains.edu.kotlin.cognifire.utils

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.CognifireDslPackageCallChecker

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


internal fun PsiElement.isPromptBlock() = text.startsWith(PROMPT) &&
                                          CognifireDslPackageCallChecker.isCallFromCognifireDslPackage(this, this.language)

const val PROMPT = "prompt"
const val CODE = "code"
