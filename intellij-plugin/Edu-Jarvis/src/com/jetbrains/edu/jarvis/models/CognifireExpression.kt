package com.jetbrains.edu.jarvis.models

interface CognifireExpression {
  var dynamicOffset: Int
  fun shiftOffset(delta: Int)
}