package com.jetbrains.edu.jarvis.highlighting

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
  private val descriptionToDraftHighlighters = mutableListOf<RangeHighlighter>()

  private val markupModel: MarkupModel?
    get() = project.selectedEditor?.markupModel

  fun clearAll() {
    grammarHighlighters.clearAndDispose()
    descriptionToDraftHighlighters.clearAndDispose()
  }

  fun clearDescriptionToDraftHighlighters() = descriptionToDraftHighlighters.clearAndDispose()

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

  fun addDescriptionToDraftHighlighter(lineNumber: Int, attributes: TextAttributes): RangeHighlighter? =
    markupModel?.addLineHighlighter(
      lineNumber,
      HighlighterLayer.LAST,
      attributes
    )?.also {
      descriptionToDraftHighlighters.add(it)
    }

  companion object {
    fun getInstance(project: Project): HighlighterManager = project.service()
  }
}