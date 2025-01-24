package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.*
import com.intellij.ui.JBColor

class GuardBlockHighlighter(private val startOffset: Int, private val endOffset: Int) : ProdeHighlighter {
  override val attributes: TextAttributes = TextAttributes().apply {
    val scheme = EditorColorsManager.getInstance().globalScheme
    backgroundColor = scheme
                        .getColor(EditorColors.READONLY_BACKGROUND_COLOR)
                       ?: DEFAULT_COLOR
  }
  override var markupHighlighter: RangeHighlighter? = null

  override fun addMarkupHighlighter(markupModel: MarkupModel?): RangeHighlighter? {
    return markupModel?.addRangeHighlighter(
      startOffset,
      endOffset,
      HighlighterLayer.GUARDED_BLOCKS,
      attributes,
      HighlighterTargetArea.EXACT_RANGE
    )?.also {
      markupHighlighter = it
    }
  }

  companion object {
    private val DEFAULT_COLOR = JBColor(0xCFE7FF, 0x2B2D30)
  }
}