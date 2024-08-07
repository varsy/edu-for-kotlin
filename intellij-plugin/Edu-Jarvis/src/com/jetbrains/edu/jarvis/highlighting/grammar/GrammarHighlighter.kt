package com.jetbrains.edu.jarvis.highlighting.grammar

import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jarvis.grammar.OffsetSentence
import com.jetbrains.edu.jarvis.highlighting.HighlighterManager

/**
 * Represents a class that highlights sentences that didn't pass the grammar.
 *
 * @property project The project in which the currently edited file is located.
 */
class GrammarHighlighter(private val project: Project) {
  fun highlightAll(unparsableSentences: List<OffsetSentence>) {
    unparsableSentences.forEach { highlightSentence(it) }
  }

  private fun highlightSentence(sentence: OffsetSentence) {
    HighlighterManager.getInstance(project).addRangeHighlighter(
      sentence.startOffset,
      sentence.endOffset,
      attributes
    )
  }

  companion object {
    private val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)
  }
}