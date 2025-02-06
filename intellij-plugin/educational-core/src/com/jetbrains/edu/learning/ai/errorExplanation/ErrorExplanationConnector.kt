package com.jetbrains.edu.learning.ai.errorExplanation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.educational.ml.core.grazie.GrazieClient
import com.jetbrains.educational.ml.core.grazie.SupportedLLMProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

@Service(Service.Level.APP)
class ErrorExplanationConnector(private val scope: CoroutineScope) : GrazieClient(ERROR_EXPLANATION_LLM_PROMPT_ID, SupportedLLMProfile.GPT4o) {
  @RequiresBackgroundThread
  fun getErrorExplanation(programmingLanguage: String, code: String, stderr: String): String {
    val systemPrompt = ErrorExplanationPromptProvider.getInstance().getSystemTemplate()
    val userPrompt = ErrorExplanationPromptProvider.getInstance().getUserTemplate(programmingLanguage, code, stderr)
    return runBlockingCancellable {
      scope.async { askGrazie(systemPrompt, userPrompt) }.await()
    }
  }

  private suspend fun askGrazie(systemPrompt: String, userPrompt: String): String {
    return withContext(Dispatchers.IO) {
      grazie.chat(systemPrompt, userPrompt)
    }
  }

  companion object {
    private val LOG = logger<ErrorExplanationConnector>()

    private const val ERROR_EXPLANATION_LLM_PROMPT_ID = "edu-error-explanation-prompt"

    fun getInstance(): ErrorExplanationConnector = service()
  }
}