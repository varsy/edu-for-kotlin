package com.jetbrains.edu.learning.ai.errorExplanation

import com.jetbrains.educational.ml.core.context.Context

data class ErrorExplanationContext(
  val programmingLanguage: String,
  val code: String,
  val stderr: String,
) : Context