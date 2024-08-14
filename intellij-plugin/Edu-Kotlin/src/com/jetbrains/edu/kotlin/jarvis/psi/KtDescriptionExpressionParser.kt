package com.jetbrains.edu.kotlin.jarvis.psi

import ai.grazie.utils.dropPostfix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.jarvis.models.FunctionSignature
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import com.jetbrains.edu.jarvis.models.DescriptionExpression
import com.jetbrains.edu.jarvis.models.FunctionArgument
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtDescriptionExpressionParser : DescriptionExpressionParser {
  override fun parseDescriptionExpression(descriptionExpression: PsiElement): DescriptionExpression? {
    if (!descriptionExpression.isDescriptionBlock() ||
        descriptionExpression !is KtCallExpression ||
        existsNestedDescriptionExpressions(descriptionExpression)
    ) {
      return null
    }
    val containingFunction = descriptionExpression.findParentOfType<KtNamedFunction>() ?: return null

    val descriptionPromptPsi = descriptionExpression.valueArguments.firstOrNull()
    val descriptionCodeBlockPsi = descriptionExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()

    val descriptionPromptText = descriptionPromptPsi?.text ?: ""
    val trimmedDescriptionPromptText = descriptionPromptText.trimStart(QUOTE_CHAR).trimStart()

    val trimmedOffset = descriptionPromptText.length - trimmedDescriptionPromptText.length

    return DescriptionExpression(
      getFunctionSignature(containingFunction),
      (descriptionPromptPsi?.textOffset ?: 0) + trimmedOffset,
      trimmedDescriptionPromptText.dropPostfix(TRIM_INDENT_POSTFIX).dropPostfix(QUOTE_POSTFIX),
      descriptionCodeBlockPsi?.bodyExpression?.text ?: ""
    )
  }

  private fun getFunctionSignature(containingFunction: KtNamedFunction): FunctionSignature {
    val containingFunctionParameters =
      containingFunction.valueParameterList?.parameters
        ?.map {
          FunctionArgument(
            it?.name ?: "",
            it?.typeReference?.name ?: ""
          )
        } ?: emptyList()

    return FunctionSignature(
      containingFunction.name ?: "",
      containingFunctionParameters,
      containingFunction.typeReference?.text ?: "Unit"
    )
  }

  private fun existsNestedDescriptionExpressions(descriptionExpression: KtCallExpression) =
    PsiTreeUtil.findChildrenOfType(descriptionExpression, KtCallExpression::class.java).any { it.isDescriptionBlock() }

  companion object {
    private const val QUOTE_CHAR = '"'
    private const val TRIM_INDENT_POSTFIX = ".trimIndent()"
    private const val QUOTE_POSTFIX = "\"\"\""
  }
}
