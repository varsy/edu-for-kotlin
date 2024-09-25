package com.jetbrains.edu.cognifire.models

class PromptExpression(
  val functionSignature: FunctionSignature,
  private val baseContentOffset: Int,
  private val baseStartOffset: Int,
  private val baseEndOffset: Int,
  val prompt: String,
  val code: String
) : CognifireExpression {
  override var dynamicOffset: Int = 0

  override val contentOffset: Int
    get() = baseContentOffset + dynamicOffset

  override val startOffset: Int
    get() = baseStartOffset + dynamicOffset

  override val endOffset: Int
    get() = baseEndOffset + dynamicOffset

  override fun shiftOffset(delta: Int) {
    dynamicOffset += delta
  }
}
