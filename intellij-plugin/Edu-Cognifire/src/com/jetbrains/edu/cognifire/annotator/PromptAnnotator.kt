package com.jetbrains.edu.cognifire.annotator

import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRule
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRule.*
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRuleMatch

interface PromptAnnotator : Annotator {
  /**
   * Returns the [PsiElement] representing the prompt block.
   * May return `null` if there is no prompt.
   */
  fun getPromptContentOrNull(element: PsiElement): PsiElement?

  fun PsiElement.getStartOffset(): Int
  fun PsiElement.getEndOffset(): Int

  /**
   * Returns a sequence of [MatchGroup] representing the parts of `target`
   * string that are relevant based on the given [Regex].
   */
  fun getAnnotatorRuleMatches(target: String, rule: AnnotatorRule): Sequence<AnnotatorRuleMatch> =
    rule.regex.findAll(target).map { it.toAnnotatorRuleMatch(rule) }

  /**
   * Converts [MatchResult] to [AnnotatorRuleMatch].
   * The [MatchResult] must contain at least one non-null capturing group.
   */
  private fun MatchResult.toAnnotatorRuleMatch(rule: AnnotatorRule): AnnotatorRuleMatch =
    when (rule) {
      KEYWORD -> AnnotatorRuleMatch(
        rule,
        identifier = groups[1] ?: error("Invalid regular expression. There should be at least one non-null capturing group."),
        keywords = listOfNotNull(groups[1])
      )
      VALUE -> AnnotatorRuleMatch(
        rule,
        identifier = groups[1] ?: error("Invalid regular expression. There should be at least one non-null capturing group."),
        values = listOfNotNull(groups[1])
      )
      STRING -> AnnotatorRuleMatch(
        rule,
        identifier = groups[1] ?: error("Invalid regular expression. There should be at least one non-null capturing group."),
        strings = listOfNotNull(groups[1])
      )
      ISOLATED_CODE -> AnnotatorRuleMatch(
        rule,
        identifier = groups[1] ?: error("Invalid regular expression. There should be at least one non-null capturing group.")
      )
      STORE_VARIABLE, SAVE_VARIABLE -> AnnotatorRuleMatch(
        rule,
        identifier = groups[4] ?: error("Invalid regular expression."),
        keywords = listOfNotNull(groups[1], groups[3]),
        values = listOfNotNull(groups[2])
      )
      SET_VARIABLE -> AnnotatorRuleMatch(
        rule, identifier = groups[2] ?: error("Invalid regular expression."),
        keywords = listOfNotNull(groups[1], groups[3]),
        values = listOfNotNull(groups[4])
      )
      LOOP_EXPRESSION -> AnnotatorRuleMatch(
        rule,
        identifier = groups[2] ?: error("Invalid regular expression."),
        keywords = listOfNotNull(groups[1], groups[3])
      )
      CREATE_VARIABLE -> AnnotatorRuleMatch(
        rule,
        identifier = (if (groups.size > 3) groups[3] else groups[2]) ?: error("Invalid regular expression."),
        keywords = listOfNotNull(groups[1]), values = listOfNotNull(groups[2])
      )
      CALL_FUNCTION -> AnnotatorRuleMatch(
        rule,
        identifier = groups[2] ?: error("Invalid regular expression."),
        if (groups.size > 3) groups[3]?.value else null,
        keywords = listOfNotNull(groups[1]),
        values = listOfNotNull(groups[3])
      )
    }
}
