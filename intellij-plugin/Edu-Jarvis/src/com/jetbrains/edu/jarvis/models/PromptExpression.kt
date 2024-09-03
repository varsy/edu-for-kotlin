package com.jetbrains.edu.jarvis.models

class PromptExpression(
  val functionSignature: FunctionSignature,
  private val baseContentOffset: Int,
  val prompt: String,
  val code: String
) : CognifireExpression {
  override var dynamicOffset: Int = 0

  override val contentOffset: Int
    get() = baseContentOffset + dynamicOffset

  override fun shiftOffset(delta: Int) {
    dynamicOffset += delta
  }
}
