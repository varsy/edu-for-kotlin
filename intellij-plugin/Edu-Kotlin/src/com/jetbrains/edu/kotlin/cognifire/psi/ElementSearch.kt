package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.CognifireDslPackageCallChecker
import com.jetbrains.edu.cognifire.utils.CODE
import com.jetbrains.edu.cognifire.utils.PROMPT
import com.jetbrains.edu.kotlin.cognifire.utils.findBlock
import org.jetbrains.kotlin.psi.KtCallExpression

object ElementSearch {

  fun findCodeElement(element: PsiElement, step: (PsiElement) -> PsiElement?): KtCallExpression? =
    findBlock(element, step) { it.isCodeElement() } as? KtCallExpression

  fun findPromptElement(element: PsiElement, step: (PsiElement) -> PsiElement?): PsiElement? =
    findBlock(element, step) { it.isPromptElement() }

  private fun PsiElement.isCodeElement() = runReadAction {
    text.startsWith(CODE) && CognifireDslPackageCallChecker.isCallFromCognifireDslPackage(this, language)
  }

  private fun PsiElement.isPromptElement() = runReadAction {
    text.startsWith(PROMPT) && CognifireDslPackageCallChecker.isCallFromCognifireDslPackage(this, language)
  }
}
