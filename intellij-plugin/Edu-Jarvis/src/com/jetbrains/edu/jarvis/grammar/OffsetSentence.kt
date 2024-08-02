package com.jetbrains.edu.jarvis.grammar

/**
 * Represents a sentence with an offset.
 * @param sentence The string containing the sentence itself.
 * @param fileOffset The offset from the beginning of the file to the beginning of the sentence.
 */
class OffsetSentence(val sentence: String, fileOffset: Int) {
  val startOffset: Int
  val endOffset: Int
  init {
    val trimmedLength = sentence.trimStart().length
    val trimmedOffset = sentence.length - trimmedLength

    startOffset = fileOffset + trimmedOffset
    endOffset = fileOffset + trimmedOffset + sentence.trim().length
  }
}
