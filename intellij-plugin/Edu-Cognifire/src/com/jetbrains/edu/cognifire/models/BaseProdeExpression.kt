package com.jetbrains.edu.cognifire.models

interface BaseProdeExpression {
  var dynamicStartOffset: Int
  var dynamicEndOffset: Int
  val contentOffset: Int
  val startOffset: Int
  val endOffset: Int
  fun shiftStartOffset(delta: Int)
  fun shiftEndOffset(delta: Int)
}