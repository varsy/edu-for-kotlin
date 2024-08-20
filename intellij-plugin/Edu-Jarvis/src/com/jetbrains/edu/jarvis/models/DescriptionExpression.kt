package com.jetbrains.edu.jarvis.models

data class DescriptionExpression(
  val functionSignature: FunctionSignature,
  val promptOffset: Int,
  val prompt: String,
  val code: String
)
