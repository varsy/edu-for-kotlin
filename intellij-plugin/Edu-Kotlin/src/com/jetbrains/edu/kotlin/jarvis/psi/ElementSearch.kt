package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import com.jetbrains.edu.kotlin.jarvis.utils.DESCRIPTION
import com.jetbrains.edu.kotlin.jarvis.utils.DRAFT
import com.jetbrains.edu.kotlin.jarvis.utils.RETURN_DRAFT
import com.jetbrains.edu.kotlin.jarvis.utils.findBlock
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

object ElementSearch {
  fun findReturnDraftElement(element: PsiElement, step: (PsiElement) -> PsiElement?): KtReturnExpression?
    = findBlock(element, step) { it.isReturnDraftElement() } as? KtReturnExpression

  fun findDraftElement(element: PsiElement, step: (PsiElement) -> PsiElement?): PsiElement?
    = findBlock(element, step) { it.isDraftElement() }

  fun findDescriptionElement(element: PsiElement, step: (PsiElement) -> PsiElement?): PsiElement?
    = findBlock(element, step) { it.isDescriptionElement() }

  private fun PsiElement.isDraftElement() =
    text.startsWith(DRAFT) && JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, language)

  private fun PsiElement.isDescriptionElement() =
    text.startsWith(DESCRIPTION) && JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(this, language)

  private fun PsiElement?.isReturnDraftElement() =
    this is KtReturnExpression
    && text.startsWith(RETURN_DRAFT)
    && getDraftBlock()?.let { JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(it, it.language) } == true

  fun PsiElement.getDraftBlock() = children.firstOrNull()?.getChildOfType<KtCallExpression>()
}