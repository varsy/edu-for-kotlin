package com.jetbrains.edu.jarvis.grammar

import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.TextAttributes

/**
 * Represents a sentence with an offset.
 * @param sentence The string containing the sentence itself.
 * @param fileOffset The offset from the beginning of the file to the beginning of the sentence.
 */
class OffsetSentence(val sentence: String, fileOffset: Int) {
  private val startOffset: Int
  private val endOffset: Int
  init {
    val trimmedLength = sentence.trimStart().length
    val trimmedOffset = sentence.length - trimmedLength

    startOffset = fileOffset + trimmedOffset
    endOffset = fileOffset + trimmedOffset + sentence.trim().length
  }

  /**
   * Highlights the sentence with a given markupModel and attributes.
   */
  fun highlight(markupModel: MarkupModel?, attributes: TextAttributes) = markupModel?.addRangeHighlighter(
    startOffset,
    endOffset,
    HighlighterLayer.ERROR, attributes, HighlighterTargetArea.EXACT_RANGE
  )
}
