package com.jetbrains.edu.cognifire.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRule
import com.jetbrains.edu.cognifire.utils.isPromptBlock


/**
 * Highlights specific syntax elements inside the `prompt` DSL element.
 */
interface PromptSyntaxAnnotator : PromptAnnotator {

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (!element.isPromptBlock()) return
    val promptContent = getPromptContentOrNull(element) ?: return
    val startOffset = promptContent.getStartOffset()
    PromptSyntaxHighlighter.TEXT_PROMPT.highlightTextRange(holder, startOffset, promptContent.getEndOffset())
    val ruleMatches = AnnotatorRule.values().asSequence()
      .flatMap { rule ->
        getAnnotatorRuleMatches(promptContent.text, rule)
      }.distinctBy { match ->
        match.identifier.range.first
      }
    val allRuleMatches = ruleMatches.flatMap { listOf(it.identifier) + it.keywords + it.values + it.strings }.distinctBy { it.range }.toList()
    ruleMatches.forEach { match ->
      PromptSyntaxHighlighter.values().forEach {
        it.highlightWords(match, startOffset, holder, allRuleMatches)
      }
    }
  }
}
