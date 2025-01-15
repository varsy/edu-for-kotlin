package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.parsers.CodeExpressionParser

class KtCodeExpressionParser : CodeExpressionParser {
  override fun getExpression(element: PsiElement): CodeExpression? {
    val codeExpressionElement = ElementSearch.findCodeElement(element) { it.nextSibling } ?: return null
    val codeContentElement = codeExpressionElement.lambdaArguments.firstOrNull()?.getLambdaExpression()
                               ?.bodyExpression ?: return null
    return CodeExpression(
      SmartPointerManager.createPointer(codeExpressionElement),
      SmartPointerManager.createPointer(codeContentElement),
      codeContentElement.text
    )
  }
}
