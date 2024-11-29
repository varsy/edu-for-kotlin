package com.jetbrains.edu.cognifire.highlighting.highlighers

import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes

interface ProdeHighlighter {
  val attributes: TextAttributes
  var markupHighlighter: RangeHighlighter?
  fun addMarkupHighlighter(markupModel: MarkupModel?): RangeHighlighter?
}