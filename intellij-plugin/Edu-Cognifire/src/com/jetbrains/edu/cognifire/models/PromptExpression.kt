package com.jetbrains.edu.cognifire.models

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

class PromptExpression(
  private val expressionElement: SmartPsiElementPointer<PsiElement>,
  private val contentElement: SmartPsiElementPointer<PsiElement>,
  val functionSignature: FunctionSignature,
  val prompt: String,
  val code: String
) : BaseProdeExpression {
  override val contentOffset: Int
    get() = contentElement.range?.startOffset?.plus("\"\"\"\n".length) ?: 0

  override val startOffset: Int
    get() = expressionElement.range?.startOffset ?: 0

  override val endOffset: Int
    get() = expressionElement.range?.endOffset ?: 0
}
