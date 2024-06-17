package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionAnnotatorResult
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.codeBlockRegex
import com.jetbrains.edu.jarvis.errors.AnnotatorError
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

class KtDescriptionErrorAnnotator : DescriptionErrorAnnotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.isRelevant()) return

    val descriptionContent = element.getChildOfType<KtValueArgumentList>() ?: return
    applyAnnotation(descriptionContent, holder)
  }

  override fun PsiElement.isRelevant() = isDescriptionBlock()

  override fun getIncorrectParts(context: String) =
    codeBlockRegex
      .findAll(context)
      .mapNotNull { it.groups[1] }
      .map {
        DescriptionAnnotatorResult(
          it.range,
          it.value.getError()
        )
      }
      .filter { it.error != AnnotatorError.NONE }

  private fun String.getError(): AnnotatorError {
    // TODO: Actually detect errors
    return if (this.lowercase().contains("bad")) AnnotatorError.PLACEHOLDER_ERROR
    else AnnotatorError.NONE
  }
}
