package com.jetbrains.edu.learning.courseFormat.jarvis

data class DescriptionExpression(
  val functionSignature: String,
  val promptOffset: Int,
  val prompt: String,
  val code: String
)
