package com.jetbrains.edu.cognifire.models

interface CognifireExpression {
  var dynamicOffset: Int
  val contentOffset: Int
  val startOffset: Int
  val endOffset: Int
  fun shiftOffset(delta: Int)
}