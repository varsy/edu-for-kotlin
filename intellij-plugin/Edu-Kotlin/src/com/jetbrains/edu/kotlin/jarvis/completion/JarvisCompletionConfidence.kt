package com.jetbrains.edu.kotlin.jarvis.completion

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThreeState
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import com.jetbrains.edu.kotlin.jarvis.utils.DESCRIPTION
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry


/**
 * Decides whether the autocomplete functionality should be skipped or not within a given context for the Jarvis application.
 * If the current element is part of a string literal in a `description` method call, the autocomplete functionality should not be skipped.
 *
 * @see CompletionConfidence
 */
class JarvisCompletionConfidence : CompletionConfidence() {
  override fun shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState {
    val functionElement = contextElement.parent.parent.parent.parent.parent
    if (contextElement.parent is KtLiteralStringTemplateEntry &&
        JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(functionElement, functionElement.language) &&
        functionElement.text.startsWith(DESCRIPTION)) {
      return ThreeState.NO
    }

    return ThreeState.UNSURE
  }
}
