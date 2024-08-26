package com.jetbrains.edu.jarvis.models

class DraftExpression (
  val code: String,
  private val baseCodeOffset: Int,
  private val baseStartOffset: Int,
  private val baseEndOffset: Int
): CognifireExpression {
  override var dynamicOffset: Int = 0

  val codeOffset: Int
    get() = baseCodeOffset + dynamicOffset

  val startOffset: Int
    get() = baseStartOffset + dynamicOffset

  val endOffset: Int
    get() = baseEndOffset + dynamicOffset

  override fun shiftOffset(delta: Int) {
    dynamicOffset += delta
  }
}
