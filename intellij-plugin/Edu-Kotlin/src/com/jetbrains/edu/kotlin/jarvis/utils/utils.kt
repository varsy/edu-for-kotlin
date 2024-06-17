package com.jetbrains.edu.kotlin.jarvis.utils

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker

fun findBlock(
  element: PsiElement,
  nextElement: (PsiElement) -> PsiElement?,
  blockStartText: String
): PsiElement? {
  var possibleBlock: PsiElement? = element
  do {
    possibleBlock = possibleBlock?.let { nextElement(it) }
  }
  while (
    possibleBlock != null &&
    !(possibleBlock.text.startsWith(blockStartText) &&
      JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(possibleBlock, possibleBlock.language))
  )
  return possibleBlock
}

internal fun PsiElement.isDescriptionBlock() = text.startsWith(DESCRIPTION) &&
                                               JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, this.language)

const val DRAFT = "draft"
const val DESCRIPTION = "description"
