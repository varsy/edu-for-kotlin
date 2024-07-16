package com.jetbrains.edu.learning.courseFormat.jarvis

data class DescriptionExpression(
  val promptOffset: Int,
  val prompt: String,
  val codeBlockOffset: Int,
  val codeBlock: String
)
