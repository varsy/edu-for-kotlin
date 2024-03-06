package com.jetbrains.edu.learning.courseFormat.eduAssistant

data class AuthorSolutionContext(
  val functionsToStringMap: Map<String, List<String>> = emptyMap(),
  val functionSignatures: Set<FunctionSignature> = emptySet()
)
