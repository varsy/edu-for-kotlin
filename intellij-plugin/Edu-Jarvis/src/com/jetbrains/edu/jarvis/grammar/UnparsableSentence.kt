package com.jetbrains.edu.jarvis.grammar

/**
 * Represents a sentence that cannot be parsed with grammar.
 * @param start offset from the beginning of the file to the start of the sentence
 * @param end offset from the beginning of the file to the end of the sentence
 */
data class UnparsableSentence(val start: Int, val end: Int)
