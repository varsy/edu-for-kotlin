package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes

class GrammarHighlighter(
  private val lineNumber: Int
) : ProdeHighlighter {
  override val attributes: TextAttributes =
    EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)

  override var markupHighlighter: RangeHighlighter? = null

  override fun addMarkupHighlighter(markupModel: MarkupModel?): RangeHighlighter? =
    markupModel?.addLineHighlighter(
      lineNumber,
      HighlighterLayer.ERROR,
      attributes,
    ).also {
      markupHighlighter = it
    }
}