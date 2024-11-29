package com.jetbrains.edu.cognifire.parsers

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.models.CognifireExpression

interface ExpressionParser<T : CognifireExpression> {
  fun getExpression(element: PsiElement): T?
}