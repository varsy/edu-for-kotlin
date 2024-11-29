package com.jetbrains.edu.kotlin.cognifire.psi

import ai.grazie.nlp.utils.dropLastWhitespaces
import ai.grazie.utils.dropPostfix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.jetbrains.edu.cognifire.parsers.PromptExpressionParser
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.models.FunctionArgument
import com.jetbrains.edu.cognifire.utils.isPromptBlock
import com.jetbrains.edu.kotlin.cognifire.utils.QUOTE_CHAR
import com.jetbrains.edu.kotlin.cognifire.utils.UNIT_RETURN_VALUE
import com.jetbrains.edu.kotlin.cognifire.utils.getBaseContentOffset
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class KtPromptExpressionParser : PromptExpressionParser {
  override fun getExpression(element: PsiElement): PromptExpression? {
    if (!element.isPromptBlock() ||
        element !is KtCallExpression ||
        existsNestedPromptExpressions(element)
    ) {
      return null
    }
    val containingFunction = element.findParentOfType<KtNamedFunction>() ?: return null

    val promptPromptPsi = element.valueArguments.firstOrNull()
    val promptCodeBlockPsi = element.lambdaArguments.firstOrNull()?.getLambdaExpression()

    val promptPromptText = promptPromptPsi?.text ?: ""
    val trimmedPromptPromptText = promptPromptText.trimStart(QUOTE_CHAR).trimStart()

    return PromptExpression(
      getFunctionSignature(containingFunction),
      promptPromptPsi?.getBaseContentOffset() ?: 0,
      element.startOffset,
      element.endOffset,
      trimmedPromptPromptText
        .dropPostfix(TRIM_INDENT_POSTFIX)
        .dropPostfix(QUOTE_POSTFIX)
        .dropLastWhitespaces(),
      promptCodeBlockPsi?.bodyExpression?.text ?: ""
    )
  }

  private fun getFunctionSignature(containingFunction: KtNamedFunction): FunctionSignature {
    val containingFunctionParameters =
      containingFunction.valueParameterList?.parameters
        ?.map {
          FunctionArgument(
            it?.name ?: "",
            it?.typeReference?.text ?: ""
          )
        } ?: emptyList()

    return FunctionSignature(
      containingFunction.name ?: "",
      containingFunctionParameters,
      containingFunction.typeReference?.text ?: UNIT_RETURN_VALUE
    )
  }

  private fun existsNestedPromptExpressions(promptExpression: KtCallExpression) =
    PsiTreeUtil.findChildrenOfType(promptExpression, KtCallExpression::class.java).any { it.isPromptBlock() }

  companion object {
    private const val TRIM_INDENT_POSTFIX = ".trimIndent()"
    private const val QUOTE_POSTFIX = "\"\"\""
  }
}
