package com.jetbrains.edu.cognifire.models

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

open class CodeExpression(
  private val expressionElement: SmartPsiElementPointer<PsiElement>,
  private val contentElement: SmartPsiElementPointer<PsiElement>,
  open val code: String
) : BaseProdeExpression {
  override val contentOffset: Int
    get() = contentElement.range?.startOffset ?: 0

  override val startOffset: Int
    get() = expressionElement.range?.startOffset ?: 0

  override val endOffset: Int
    get() = expressionElement.range?.endOffset ?: 0
}
