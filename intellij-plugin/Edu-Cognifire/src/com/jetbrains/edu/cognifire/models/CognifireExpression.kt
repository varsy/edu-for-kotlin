package com.jetbrains.edu.cognifire.models

interface CognifireExpression {
  var dynamicOffset: Int
  val contentOffset: Int
  fun shiftOffset(delta: Int)
}