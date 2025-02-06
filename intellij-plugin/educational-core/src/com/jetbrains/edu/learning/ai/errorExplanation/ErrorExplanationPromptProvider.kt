package com.jetbrains.edu.learning.ai.errorExplanation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.educational.ml.core.prompt.PromptProvider

@Service(Service.Level.APP)
class ErrorExplanationPromptProvider : PromptProvider() {
  fun getSystemTemplate(): String = ErrorExplanationPromptTemplate.SYSTEM_PROMPT.process()
  fun getUserTemplate(programmingLanguage: String, code: String, stderr: String): String {
    return ErrorExplanationPromptTemplate.USER_PROMPT.process(ErrorExplanationContext(programmingLanguage, code, stderr))
  }

  companion object {
    fun getInstance(): ErrorExplanationPromptProvider = service()
  }
}