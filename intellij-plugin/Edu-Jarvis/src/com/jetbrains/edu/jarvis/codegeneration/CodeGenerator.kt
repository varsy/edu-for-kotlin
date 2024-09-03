package com.jetbrains.edu.jarvis.codegeneration

import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.jarvis.models.PromptExpression
import com.jetbrains.educational.ml.cognifire.core.PromptToCodeAssistant
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

class CodeGenerator(promptExpression: PromptExpression) {
  private val enumeratedPromptLines = getEnumeratedPromptLines(promptExpression.prompt)

  private val promptToCodeTranslation: PromptToCodeResponse =
    getCodeFromPrompt(promptExpression.functionSignature.toString(), enumeratedPromptLines)
  val promptToCodeLines = promptToCodeTranslation
    .groupBy { it.promptLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.codeLineNumber }
    }
  val codeToPromptLines = promptToCodeTranslation
    .groupBy { it.codeLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.promptLineNumber }
    }
  val generatedCode =
    promptToCodeTranslation
      .distinctBy { it.codeLineNumber }
      .joinToString(System.lineSeparator()) {
        it.generatedCodeLine
      }

  private fun getCodeFromPrompt(functionSignature: String, enumeratedPromptLines: String) = runBlockingCancellable {
    PromptToCodeAssistant.generateCode(
      enumeratedPromptLines,
      functionSignature
    )
      .getOrThrow()
  }

  private fun getEnumeratedPromptLines(prompt: String) =
    prompt.lines().mapIndexed { index, line -> "$index: $line" }.joinToString(System.lineSeparator())
}