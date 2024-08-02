package com.jetbrains.edu.jarvis.highlighting

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.selectedEditor

@Service(Service.Level.PROJECT)
class HighlighterManager(private val project: Project) {
  private val grammarHighlighters = mutableListOf<RangeHighlighter>()
  private val codeLineHighlighters = mutableListOf<RangeHighlighter>()
  private var descriptionLineHighlighter: RangeHighlighter? = null

  private val markupModel: MarkupModel?
    get() = project.selectedEditor?.markupModel

  fun clearHighlighters() {
    clearGrammarHighlighters()
    clearLineHighlighters()
  }

  private fun clearGrammarHighlighters() {
    grammarHighlighters.forEach { it.dispose() }
    grammarHighlighters.clear()
  }

  fun clearLineHighlighters() {
    codeLineHighlighters.forEach { it.dispose() }
    descriptionLineHighlighter?.dispose()
    descriptionLineHighlighter = null
    grammarHighlighters.clear()
  }

  fun addGrammarHighlighter(startOffset: Int, endOffset: Int, attributes: TextAttributes) {
    markupModel?.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.ERROR, attributes, HighlighterTargetArea.EXACT_RANGE
    )?.let {
      grammarHighlighters.add(it)
    }
  }

  fun addCodeLineHighlighter(lineNumber: Int, attributes: TextAttributes) {
    markupModel?.addLineHighlighter(
      lineNumber,
      HighlighterLayer.LAST,
      attributes
    )?.let {
      codeLineHighlighters.add(it)
    }
  }

  fun addDescriptionLineHighlighter(lineNumber: Int, attributes: TextAttributes) {
    descriptionLineHighlighter = markupModel?.addLineHighlighter(
      lineNumber,
      HighlighterLayer.LAST,
      attributes
    )
  }

  fun hasGrammarHighlighters(): Boolean = !grammarHighlighters.isEmpty()

  companion object {
    fun getInstance(project: Project): HighlighterManager = project.service()
  }
}