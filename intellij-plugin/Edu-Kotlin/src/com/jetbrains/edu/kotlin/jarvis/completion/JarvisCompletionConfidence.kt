package com.jetbrains.edu.kotlin.jarvis.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import com.jetbrains.edu.kotlin.jarvis.psi.ElementSearch
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry


/**
 * Decides whether the autocomplete functionality should be skipped or not within a given context for the Jarvis application.
 * If the current element is part of a string literal in a `description` method call, the autocomplete functionality shouldn't be skipped.
 *
 * @see CompletionConfidence
 */
class JarvisCompletionConfidence : CompletionConfidence() {
  override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
    val descriptionBlock = ElementSearch.findDescriptionElement(contextElement)
    if (descriptionBlock != null && contextElement.parent is KtLiteralStringTemplateEntry) {
      return ThreeState.NO
    }

    return ThreeState.UNSURE
  }
}
