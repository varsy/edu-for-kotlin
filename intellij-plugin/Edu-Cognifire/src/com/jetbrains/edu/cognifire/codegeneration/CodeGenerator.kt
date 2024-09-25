package com.jetbrains.edu.cognifire.codegeneration

import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.educational.ml.cognifire.core.PromptToCodeAssistant
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

class CodeGenerator(promptExpression: PromptExpression) {
  private val enumeratedPromptLines = getEnumeratedPromptLines(promptExpression)

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

  private fun getEnumeratedPromptLines(promptExpression: PromptExpression) =
    listOf(promptExpression.prompt, promptExpression.code).joinToString(System.lineSeparator())
      .lines().mapIndexed { index, line -> "$index: $line" }.joinToString(System.lineSeparator())
}