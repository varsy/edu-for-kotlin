package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.models.SimpleCodeExpression
import com.jetbrains.edu.cognifire.parsers.CodeExpressionParser

class KtCodeExpressionParser : CodeExpressionParser {
  override fun getExpression(element: PsiElement) =
    ElementSearch.findCodeElement(element) { it.nextSibling }?.lambdaArguments?.firstOrNull()?.getLambdaExpression()
      ?.bodyExpression?.text?.let { SimpleCodeExpression(it) }
}
