package com.jetbrains.edu.cognifire.highlighting.grammar

import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.grammar.OffsetSentence
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager

private val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)

/**
 * Represents a class that highlights sentences that didn't pass the grammar.
 */
object GrammarHighlighter {
  fun highlightAll(project: Project, unparsableSentences: List<OffsetSentence>) {
    unparsableSentences.forEach { highlightSentence(project, it) }
  }

  private fun highlightSentence(project: Project, sentence: OffsetSentence) {
    HighlighterManager.getInstance(project).addGrammarHighlighter(
      sentence.startOffset,
      sentence.endOffset,
      attributes
    )
  }
}