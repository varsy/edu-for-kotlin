package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.*

class GrammarHighlighter(
  private val startOffset: Int,
  private val endOffset: Int
) : ProdeHighlighter {
  override val attributes: TextAttributes =
    EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)

  override var markupHighlighter: RangeHighlighter? = null

  override fun addMarkupHighlighter(markupModel: MarkupModel?): RangeHighlighter? =
    markupModel?.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.ERROR,
      attributes,
      HighlighterTargetArea.EXACT_RANGE
    ).also {
      markupHighlighter = it
    }
}