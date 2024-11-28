package com.jetbrains.edu.cognifire.annotator

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRuleMatch

// TODO: change colour
enum class PromptSyntaxHighlighter(
  private val customName: String,
  private val colorKey: TextAttributesKey,
  private val extractWords: (AnnotatorRuleMatch) -> List<MatchGroup>,
  private val startShift: Int = 0,
  private val endShift: Int = 0
) {
  TEXT_PROMPT("TEXT_PROMPT_HIGHLIGHT", DefaultLanguageHighlighterColors.LINE_COMMENT, { emptyList() }),
  IDENTIFIER("IDENTIFIER_HIGHLIGHT", DefaultLanguageHighlighterColors.IDENTIFIER, { listOf(it.identifier) }, - 1, 1),
  KEYWORD("KEYWORD_HIGHLIGHT", DefaultLanguageHighlighterColors.KEYWORD, { it.keywords }),
  VALUE("VALUE_HIGHLIGHT", DefaultLanguageHighlighterColors.NUMBER, { it.values }),
  STRING("STRING_HIGHLIGHT", DefaultLanguageHighlighterColors.STRING, { it.strings });

  fun highlightWords(ruleMatch: AnnotatorRuleMatch, startOffset: Int, holder: AnnotationHolder, allRuleMatches: List<MatchGroup>) {
    extractWords(ruleMatch).filter { word ->
      allRuleMatches.filter { it != word }.none { it.range.contains(word.range.first) && it.range.contains(word.range.last) }
    }.forEach { word ->
      holder
        .newSilentAnnotation(HighlightSeverity.INFORMATION)
        .textAttributes(TextAttributesKey.createTextAttributesKey(customName, colorKey))
        .range(
          TextRange(
            startOffset + word.range.first + startShift,
            startOffset + word.range.last + 1 + endShift
          )
        ).create()
    }
  }

  fun highlightTextRange(holder: AnnotationHolder, startOffset: Int, endOffset: Int) {
    holder
      .newSilentAnnotation(HighlightSeverity.INFORMATION)
      .textAttributes(TextAttributesKey.createTextAttributesKey(customName, colorKey))
      .range(TextRange(startOffset, endOffset))
      .create()
  }
}
