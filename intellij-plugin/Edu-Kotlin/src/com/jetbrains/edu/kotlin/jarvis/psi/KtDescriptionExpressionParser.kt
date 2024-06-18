package com.jetbrains.edu.kotlin.jarvis.psi

import ai.grazie.utils.dropPostfix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import org.jetbrains.kotlin.psi.KtCallExpression

class KtDescriptionExpressionParser : DescriptionExpressionParser {
  override fun parseDescriptionExpression(descriptionExpression: PsiElement): DescriptionExpression? {
    if (!descriptionExpression.isDescriptionBlock() ||
        descriptionExpression !is KtCallExpression ||
        existsNestedDescriptionExpressions(descriptionExpression)) {
      return null
    }
    return DescriptionExpression(
      descriptionExpression.valueArguments.firstOrNull()?.text?.dropPostfix(TRIM_INDENT_POSTFIX)?.trim(QUOTE_CHAR) ?: "",
      descriptionExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression?.text ?: ""
    )
  }

  private fun existsNestedDescriptionExpressions(descriptionExpression: KtCallExpression) =
    PsiTreeUtil.findChildrenOfType(descriptionExpression, KtCallExpression::class.java).any { it.isDescriptionBlock() }

  companion object {
    private const val QUOTE_CHAR = '"'
    private const val TRIM_INDENT_POSTFIX = ".trimIndent()"
  }
}
