package com.jetbrains.edu.jarvis.models

interface CognifireExpression {
  var dynamicOffset: Int
  val contentOffset: Int
  fun shiftOffset(delta: Int)
}