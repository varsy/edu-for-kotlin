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


internal fun PsiElement.isPromptBlock() = text.startsWith(PROMPT) &&
                                          JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, this.language)

const val PROMPT = "prompt"
const val CODE = "code"
