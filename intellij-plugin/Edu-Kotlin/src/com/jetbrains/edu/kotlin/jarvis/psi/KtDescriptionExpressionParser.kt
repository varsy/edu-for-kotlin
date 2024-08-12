package com.jetbrains.edu.kotlin.jarvis.psi

import ai.grazie.utils.dropPostfix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtDescriptionExpressionParser : DescriptionExpressionParser {
  override fun parseDescriptionExpression(descriptionExpression: PsiElement): DescriptionExpression? {
    if (!descriptionExpression.isDescriptionBlock() ||
        descriptionExpression !is KtCallExpression ||
        existsNestedDescriptionExpressions(descriptionExpression)) {
      return null
    }
    val containingFunction = descriptionExpression.findParentOfType<KtNamedFunction>() ?: return null

    val descriptionPromptPsi = descriptionExpression.valueArguments.firstOrNull()
    val descriptionCodeBlockPsi = descriptionExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()

    val descriptionPromptText = descriptionPromptPsi?.text ?: ""
    val trimmedDescriptionPromptText = descriptionPromptText.trimStart(QUOTE_CHAR).trimStart()

    val trimmedOffset = descriptionPromptText.length - trimmedDescriptionPromptText.length

    return DescriptionExpression(
      containingFunction.getSignature(),
      (descriptionPromptPsi?.textOffset ?: 0) + trimmedOffset,
      trimmedDescriptionPromptText.dropPostfix(TRIM_INDENT_POSTFIX).dropPostfix(QUOTE_POSTFIX),
      descriptionCodeBlockPsi?.bodyExpression?.text ?: ""
    )
  }

  private fun KtNamedFunction.getSignature(): String {
    val parameterListString = valueParameterList?.parameters?.joinToString(ARGUMENT_SEPARATOR) { param ->
        "${param.name}: ${param.typeReference?.text ?: ""}"
    }
    val returnType = typeReference?.text
    return "fun $name ($parameterListString)" +
           if(returnType != null) ": $returnType" else ""
  }

  private fun existsNestedDescriptionExpressions(descriptionExpression: KtCallExpression) =
    PsiTreeUtil.findChildrenOfType(descriptionExpression, KtCallExpression::class.java).any { it.isDescriptionBlock() }

  companion object {
    private const val QUOTE_CHAR = '"'
    private const val TRIM_INDENT_POSTFIX = ".trimIndent()"
    private const val QUOTE_POSTFIX = "\"\"\""
    private const val ARGUMENT_SEPARATOR = ", "
  }
}
