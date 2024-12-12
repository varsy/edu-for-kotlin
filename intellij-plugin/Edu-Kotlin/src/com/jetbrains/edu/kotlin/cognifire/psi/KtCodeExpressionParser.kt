package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.parsers.CodeExpressionParser
import com.jetbrains.edu.cognifire.models.CodeExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KtCodeExpressionParser : CodeExpressionParser {
  override fun getExpression(element: PsiElement): CodeExpression? {
    val codeElement = ElementSearch.findCodeElement(element) { it.nextSibling } ?: return null
    val code = ElementSearch.findCodeElement(element) { it.nextSibling }?.lambdaArguments?.firstOrNull()?.getLambdaExpression()
      ?.bodyExpression ?: return null
    return CodeExpression(
      code.text,
      code.textOffset,
      codeElement.startOffset,
      codeElement.endOffset
    )
  }
}
