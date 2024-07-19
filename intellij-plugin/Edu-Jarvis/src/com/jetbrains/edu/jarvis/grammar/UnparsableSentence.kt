package com.jetbrains.edu.jarvis.grammar

import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.TextAttributes

/**
 * Represents a sentence that cannot be parsed with grammar.
 * @param start The offset from the beginning of the file to the start of the sentence
 * @param end The offset from the beginning of the file to the end of the sentence
 */
class UnparsableSentence(private val start: Int, private val end: Int) {

  /**
   * Highlights the sentence with a given markupModel and attributes.
   */
  fun highlight(markupModel: MarkupModel?, attributes: TextAttributes) = markupModel?.addRangeHighlighter(
    start,
    end,
    HighlighterLayer.ERROR, attributes, HighlighterTargetArea.EXACT_RANGE
  )
}
