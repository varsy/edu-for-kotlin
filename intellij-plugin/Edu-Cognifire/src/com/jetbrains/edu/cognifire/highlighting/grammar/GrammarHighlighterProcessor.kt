package com.jetbrains.edu.cognifire.highlighting.grammar

import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.grammar.OffsetSentence
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.cognifire.highlighting.highlighers.GrammarHighlighter

/**
 * Represents a class that highlights sentences that didn't pass the grammar.
 */
object GrammarHighlighterProcessor {
  fun highlightAll(project: Project, unparsableSentences: List<OffsetSentence>) {
    unparsableSentences.forEach { highlightSentence(project, it) }
  }

  private fun highlightSentence(project: Project, sentence: OffsetSentence) {
    HighlighterManager.getInstance(project).addProdeHighlighter(
      GrammarHighlighter(sentence.startOffset, sentence.endOffset)
    )
  }
}