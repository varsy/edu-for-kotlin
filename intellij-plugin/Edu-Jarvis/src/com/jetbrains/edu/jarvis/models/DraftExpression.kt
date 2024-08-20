package com.jetbrains.edu.jarvis.models

data class DraftExpression (
  val code: String,
  val codeOffset: Int,
  val startOffset: Int,
  val endOffset: Int
)
