package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionAnnotatorResult
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator
import com.jetbrains.edu.jarvis.enums.AnnotatorRule
import com.jetbrains.edu.jarvis.enums.AnnotatorError
import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError
import com.jetbrains.edu.jarvis.models.FunctionCall
import com.jetbrains.edu.kotlin.KtErrorProcessor
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildrenOfType


class KtDescriptionErrorAnnotator : DescriptionErrorAnnotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.isRelevant()) return
    val descriptionContent = element.getChildOfType<KtValueArgumentList>() ?: return
    applyAnnotation(descriptionContent, holder)
  }

  override fun PsiElement.isRelevant() = isDescriptionBlock()

  override fun getIncorrectParts(context: PsiElement): Collection<DescriptionAnnotatorResult> {
    val visibleFunctions = context.containingFile
      .getChildrenOfType<KtNamedFunction>()
      .mapNotNull { it.toFunctionCallOrNull() }
    return AnnotatorRule.values()
      .flatMap { rule ->
        getIncorrectPartsByRegex(context, rule.regex).map {
          DescriptionAnnotatorResult(
            it.range,
            it.value.getError(rule, visibleFunctions)
          )
        }
      }
      .toSet()
      .filter { it.parametrizedError.errorType != AnnotatorError.NONE }

  }

  private fun getIncorrectPartsByRegex(context: PsiElement, regex: Regex): Sequence<MatchGroup> {
    return regex
      .findAll(context.text)
      .mapNotNull { it.groups[1] }
  }

  private fun KtNamedFunction.toFunctionCallOrNull(): FunctionCall? {
    val numberOfParameters = getChildOfType<KtParameterList>()?.parameters?.size ?: 0
    return FunctionCall(name ?: return null, numberOfParameters)
  }

  private fun String.getError(rule: AnnotatorRule, visibleFunctions: Collection<FunctionCall>): AnnotatorParametrizedError {
    val processor = KtErrorProcessor(this, visibleFunctions)
    return when(rule) {
      AnnotatorRule.ISOLATED_CODE -> {
        processor.processIsolatedCode()
      }
      AnnotatorRule.NO_PARENTHESES_FUNCTION_CALL -> {
        processor.processNoParenthesesFunctionCall()
      }
      else -> AnnotatorParametrizedError.NO_ERROR
    }
  }
}
