package com.jetbrains.edu.cognifire.parsers

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.models.BaseProdeExpression

interface ExpressionParser<T : BaseProdeExpression> {
  fun getExpression(element: PsiElement): T?
}