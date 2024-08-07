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
  private val highlighters = mutableListOf<RangeHighlighter>()

  private val markupModel: MarkupModel?
    get() = project.selectedEditor?.markupModel

  fun clearAll() {
    highlighters.forEach {
      it.dispose()
    }
    highlighters.clear()
  }

  fun addRangeHighlighter(startOffset: Int, endOffset: Int, attributes: TextAttributes): RangeHighlighter? =
    markupModel?.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.ERROR,
      attributes,
      HighlighterTargetArea.EXACT_RANGE
    )?.also {
      highlighters.add(it)
    }

  fun addLineHighlighter(lineNumber: Int, attributes: TextAttributes): RangeHighlighter? =
    markupModel?.addLineHighlighter(
      lineNumber,
      HighlighterLayer.LAST,
      attributes
    )?.also {
      highlighters.add(it)
    }

  companion object {
    fun getInstance(project: Project): HighlighterManager = project.service()
  }
}