package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.jetbrains.edu.kotlin.jarvis.utils.HIGHLIGHT_MESSAGE
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KtDescriptionAnnotator : Annotator {
  private val codeBlockRegex = "`([^`]+)`".toRegex()

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.isDescriptionBlock()) return

    val descriptionContent = element.getChildOfType<KtValueArgumentList>() ?: return

    getBadCodeBlocks(descriptionContent.text).forEach {
      val errorRange = TextRange(
        descriptionContent.startOffset + it.range.first,
        descriptionContent.startOffset + it.range.last + 1
      )
      holder.newAnnotation(HighlightSeverity.ERROR, HIGHLIGHT_MESSAGE)
        .range(errorRange)
        .create()
    }
  }

  private fun getBadCodeBlocks(descriptionText: String) =
    codeBlockRegex.findAll(descriptionText).mapNotNull { it.groups[1] }.filter { it.value.containsErrors() }

  private fun String.containsErrors(): Boolean {
    // TODO: Actually detect errors
    return this.lowercase().contains("bad")
  }
}