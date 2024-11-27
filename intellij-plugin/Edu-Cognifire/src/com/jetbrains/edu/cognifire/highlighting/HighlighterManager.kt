package com.jetbrains.edu.cognifire.highlighting

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.selectedEditor

/**
 * Manages the highlighters in the editor. Allows adding and clearing highlighters for specific ranges and lines of code.
 *
 * @param project The project in which the currently edited file is located.
 */
@Service(Service.Level.PROJECT)
class HighlighterManager(private val project: Project) {
  private val grammarHighlighters = mutableListOf<RangeHighlighter>()
  private val promptToCodeHighlighters = mutableListOf<RangeHighlighter>()
  private val uncommitedChangesHighlighters = mutableListOf<RangeHighlighter>()

  private val markupModel: MarkupModel?
    get() = project.selectedEditor?.markupModel

  fun clearAllAfterSync() {
    grammarHighlighters.clearAndDispose()
    promptToCodeHighlighters.clearAndDispose()
  }

  fun clearAll() {
    clearAllAfterSync()
    uncommitedChangesHighlighters.clearAndDispose()
  }

  fun clearPromptToCodeHighlighters() = promptToCodeHighlighters.clearAndDispose()

  private fun MutableList<RangeHighlighter>.clearAndDispose() {
    forEach {
      it.dispose()
    }
    clear()
  }

  fun addGrammarHighlighter(startOffset: Int, endOffset: Int, attributes: TextAttributes): RangeHighlighter? =
    markupModel?.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.ERROR,
      attributes,
      HighlighterTargetArea.EXACT_RANGE
    )?.also {
      grammarHighlighters.add(it)
    }

  fun addPromptToCodeHighlighter(lineNumber: Int, attributes: TextAttributes): RangeHighlighter? =
    markupModel?.addLineHighlighter(
      lineNumber,
      HighlighterLayer.LAST,
      attributes
    )?.also {
      promptToCodeHighlighters.add(it)
    }

  fun addUncommitedChangesHighlighter(startOffset: Int, endOffset: Int, attributes: TextAttributes): RangeHighlighter? =
    markupModel?.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.SELECTION,
      attributes,
      HighlighterTargetArea.EXACT_RANGE
    )?.also {
      uncommitedChangesHighlighters.add(it)
    }

  companion object {
    fun getInstance(project: Project): HighlighterManager = project.service()
  }
}