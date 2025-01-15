package com.jetbrains.edu.kotlin.cognifire.psi

import ai.grazie.nlp.utils.dropLastWhitespaces
import ai.grazie.utils.dropPostfix
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.jetbrains.edu.cognifire.models.FunctionArgument
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.parsers.PromptExpressionParser
import com.jetbrains.edu.cognifire.utils.isPromptBlock
import com.jetbrains.edu.kotlin.cognifire.utils.QUOTE_CHAR
import com.jetbrains.edu.kotlin.cognifire.utils.UNIT_RETURN_VALUE
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtPromptExpressionParser : PromptExpressionParser {
  override fun getExpression(element: PsiElement): PromptExpression? {
    if (!isValidPromptElement(element)) {
      return null
    }

    val callExpression = element as KtCallExpression
    val containingFunction = callExpression.findParentOfType<KtNamedFunction>() ?: return null

    return buildPromptExpression(callExpression, containingFunction)
  }

  private fun isValidPromptElement(element: PsiElement): Boolean =
    element.isPromptBlock() &&
    element is KtCallExpression &&
    !existsNestedPromptExpressions(element)

  private fun buildPromptExpression(
    callExpression: KtCallExpression,
    containingFunction: KtNamedFunction
  ): PromptExpression? {
    val (promptText, codeBlock) = extractPromptComponents(callExpression)
    val contentElement = callExpression.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null

    return PromptExpression(
      SmartPointerManager.createPointer(callExpression),
      SmartPointerManager.createPointer(contentElement),
      getFunctionSignature(containingFunction),
      processPromptText(promptText),
      codeBlock?.bodyExpression?.text.orEmpty()
    )
  }

  private fun extractPromptComponents(callExpression: KtCallExpression): Pair<String, KtLambdaExpression?> {
    val promptArgument = callExpression.valueArguments.firstOrNull()
    val codeBlock = callExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()

    return Pair(
      promptArgument?.text.orEmpty(),
      codeBlock
    )
  }

  private fun processPromptText(rawPromptText: String): String =
    rawPromptText
      .trimStart(QUOTE_CHAR)
      .trimStart()
      .dropPostfix(TRIM_INDENT_POSTFIX)
      .dropPostfix(QUOTE_POSTFIX)
      .dropLastWhitespaces()

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
      containingFunction.fqName?.toString() ?: "",
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
