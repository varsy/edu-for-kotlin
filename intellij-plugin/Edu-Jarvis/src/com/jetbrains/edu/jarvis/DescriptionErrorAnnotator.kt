package com.jetbrains.edu.jarvis

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle

/**
 * Highlights parts containing errors inside the `description` DSL element.
 */

interface DescriptionErrorAnnotator : Annotator {

  fun applyAnnotation(
    descriptionContent: PsiElement,
    holder: AnnotationHolder
  ) =
    getIncorrectParts(descriptionContent.text).forEach {
      val errorRange = TextRange(
        descriptionContent.startOffset + it.range.first,
        descriptionContent.startOffset + it.range.last + 1
      )
      holder.newAnnotation(HighlightSeverity.ERROR, EduJarvisBundle.message(it.error.message))
        .range(errorRange)
        .create()
    }

  /**
   * Returns a sequence of [DescriptionAnnotatorResult] which contains parts of
   * `context` to be highlighted and the type of error that the corresponding part contains.
   */
  fun getIncorrectParts(context: String): Sequence<DescriptionAnnotatorResult>

  /**
   * Returns whether the [PsiElement] is relevant, that is, whether it may contain an error.
   */
  fun PsiElement.isRelevant(): Boolean

  companion object {
    val codeBlockRegex = "`([^`]+)`".toRegex()
  }

}
