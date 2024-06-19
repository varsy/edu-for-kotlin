package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionAnnotatorResult
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.codeBlockRegex
import com.jetbrains.edu.jarvis.errors.AnnotatorError
import com.jetbrains.edu.jarvis.models.FunctionCall
import com.jetbrains.edu.kotlin.jarvis.utils.ARGUMENT_SEPARATOR
import com.jetbrains.edu.kotlin.jarvis.utils.CLOSE_PARENTHESIS
import com.jetbrains.edu.kotlin.jarvis.utils.OPEN_PARENTHESIS
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

  override fun getIncorrectParts(context: PsiElement): Sequence<DescriptionAnnotatorResult> {
    val visibleFunctions = context.containingFile
      .getChildrenOfType<KtNamedFunction>()
      .mapNotNull { it.toFunctionCallOrNull() }
    return codeBlockRegex
      .findAll(context.text)
      .mapNotNull { it.groups[1] }
      .map {
        DescriptionAnnotatorResult(
          it.range,
          it.value.getError(visibleFunctions)
        )
      }
      .filter { it.error != AnnotatorError.NONE }
  }


  private fun KtNamedFunction.toFunctionCallOrNull(): FunctionCall? {
    val numberOfParameters = getChildOfType<KtParameterList>()?.parameters?.size ?: 0
    return FunctionCall(name ?: return null, numberOfParameters)
  }

  private fun String.isAFunctionCall() =
    // TODO: check if string is a function call from surrounding context
    DescriptionErrorAnnotator.functionCallRegex.matches(this)

  private fun String.toFunctionCall(): FunctionCall {
    val functionName = this.substringBefore(OPEN_PARENTHESIS)
    val parameters = this
      .substringAfter(OPEN_PARENTHESIS)
      .substringBefore(CLOSE_PARENTHESIS)

    val numberOfParameters = if (parameters.isNotBlank()) {
      parameters.count { it == ARGUMENT_SEPARATOR } + 1
    }
    else 0

    return FunctionCall(functionName, numberOfParameters)
  }

  private fun String.getError(visibleFunctions: Collection<FunctionCall>): AnnotatorError {
    if (this.isAFunctionCall() && this.toFunctionCall() !in visibleFunctions) {
      return AnnotatorError.UNKNOWN_FUNCTION
    }
    return AnnotatorError.NONE
  }
}
