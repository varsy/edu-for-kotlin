package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.annotator.PromptSyntaxAnnotator
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KtPromptSyntaxHighlightingAnnotator : PromptSyntaxAnnotator {
  override fun getPromptContentOrNull(element: PsiElement): PsiElement? = element.getChildOfType<KtValueArgumentList>()

  override fun PsiElement.getStartOffset(): Int = startOffset
  override fun PsiElement.getEndOffset() = endOffset
}
