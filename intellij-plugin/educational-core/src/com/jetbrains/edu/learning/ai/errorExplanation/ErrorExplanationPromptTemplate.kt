package com.jetbrains.edu.learning.ai.errorExplanation

import com.jetbrains.educational.ml.core.prompt.PromptTemplate

enum class ErrorExplanationPromptTemplate(override val fileName: String) : PromptTemplate {
  SYSTEM_PROMPT("ErrorExplanationSystemPrompt.ftl"),
  USER_PROMPT("ErrorExplanationUserPrompt.ftl")
}