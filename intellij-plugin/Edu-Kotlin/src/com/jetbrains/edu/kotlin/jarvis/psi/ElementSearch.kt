package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import com.jetbrains.edu.kotlin.jarvis.utils.DESCRIPTION
import com.jetbrains.edu.kotlin.jarvis.utils.DRAFT
import com.jetbrains.edu.kotlin.jarvis.utils.findBlock
import org.jetbrains.kotlin.psi.KtCallExpression

object ElementSearch {

  fun findDraftElement(element: PsiElement, step: (PsiElement) -> PsiElement?): KtCallExpression? =
    findBlock(element, step) { it.isDraftElement() } as? KtCallExpression

  fun findDescriptionElement(element: PsiElement, step: (PsiElement) -> PsiElement?): PsiElement? =
    findBlock(element, step) { it.isDescriptionElement() }

  private fun PsiElement.isDraftElement() =
    text.startsWith(DRAFT) && JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, language)

  private fun PsiElement.isDescriptionElement() =
    text.startsWith(DESCRIPTION) && JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, language)
}
