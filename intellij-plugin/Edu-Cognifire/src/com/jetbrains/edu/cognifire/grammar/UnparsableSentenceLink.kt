package com.jetbrains.edu.cognifire.grammar

data class UnparsableSentenceLink(
  val promptIndices: Set<Int>,
  val codeIndices: Set<Int>
) {
  operator fun plus(other: UnparsableSentenceLink): UnparsableSentenceLink =
    UnparsableSentenceLink(promptIndices + other.promptIndices, codeIndices + other.codeIndices)
}