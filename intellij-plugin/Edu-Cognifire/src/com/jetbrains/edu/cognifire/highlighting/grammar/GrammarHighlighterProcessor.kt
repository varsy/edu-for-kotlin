package com.jetbrains.edu.cognifire.highlighting.grammar

import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.cognifire.highlighting.highlighers.GrammarHighlighter

/**
 * Represents a class that highlights sentences that didn't pass the grammar.
 */
object GrammarHighlighterProcessor {
  fun highlightAll(project: Project, lineIndices: Collection<Int>, actionId: String) {
    lineIndices.forEach { highlightSentence(project, it, actionId) }
  }

  private fun highlightSentence(project: Project, line: Int, actionId: String) {
    HighlighterManager.getInstance().addProdeHighlighter(GrammarHighlighter(line), actionId, project)
  }
}