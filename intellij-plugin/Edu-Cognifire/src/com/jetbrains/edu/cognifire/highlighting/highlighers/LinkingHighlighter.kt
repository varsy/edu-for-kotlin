package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.ui.JBColor

class LinkingHighlighter(private val lineNumber: Int) : ProdeHighlighter {
  override val attributes = TextAttributes().apply { backgroundColor = JBColor.LIGHT_GRAY }
  override var markupHighlighter: RangeHighlighter? = null

  override fun addMarkupHighlighter(markupModel: MarkupModel?): RangeHighlighter? =
    markupModel?.addLineHighlighter(
      lineNumber,
      HighlighterLayer.LAST,
      attributes
    ).also {
      markupHighlighter = it
    }
}