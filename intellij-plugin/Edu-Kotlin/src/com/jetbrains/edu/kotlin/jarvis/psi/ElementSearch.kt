package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import com.jetbrains.edu.kotlin.jarvis.utils.PROMPT
import com.jetbrains.edu.kotlin.jarvis.utils.CODE
import com.jetbrains.edu.kotlin.jarvis.utils.findBlock
import org.jetbrains.kotlin.psi.KtCallExpression

object ElementSearch {

  fun findCodeElement(element: PsiElement, step: (PsiElement) -> PsiElement?): KtCallExpression? =
    findBlock(element, step) { it.isCodeElement() } as? KtCallExpression

  fun findPromptElement(element: PsiElement, step: (PsiElement) -> PsiElement?): PsiElement? =
    findBlock(element, step) { it.isPromptElement() }

  private fun PsiElement.isCodeElement() =
    text.startsWith(CODE) && JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, language)

  private fun PsiElement.isPromptElement() =
    text.startsWith(PROMPT) && JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, language)
}
