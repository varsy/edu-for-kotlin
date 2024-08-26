package com.jetbrains.edu.jarvis.models

class DescriptionExpression(
  val functionSignature: FunctionSignature,
  private val basePromptOffset: Int,
  val prompt: String,
  val code: String
) : CognifireExpression {
  override var dynamicOffset: Int = 0

  val promptOffset: Int
    get() = basePromptOffset + dynamicOffset

  override fun shiftOffset(delta: Int) {
    dynamicOffset += delta
  }
}
