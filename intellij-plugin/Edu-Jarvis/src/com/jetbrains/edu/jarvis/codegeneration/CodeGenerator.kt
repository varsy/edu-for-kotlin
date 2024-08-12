package com.jetbrains.edu.jarvis.codegeneration

import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import com.jetbrains.educational.ml.cognifire.responses.DescriptionToCodeResponse
import com.jetbrains.educational.ml.cognifire.core.DescriptionToCodeAssistant

class CodeGenerator(descriptionExpression: DescriptionExpression) {
  private val enumeratedPromptLines = getEnumeratedPromptLines(descriptionExpression.prompt)

  private val descriptionToCodeTranslation: DescriptionToCodeResponse =
    getDescriptionToCode(descriptionExpression.functionSignature, enumeratedPromptLines)
  val descriptionToCodeLines = descriptionToCodeTranslation
    .groupBy { it.descriptionLineNumber }
    .mapValues { descriptionGroup ->
      descriptionGroup.value.map { it.codeLineNumber }
    }
  val generatedCode =
    descriptionToCodeTranslation
      .distinctBy { it.codeLineNumber }
      .joinToString(System.lineSeparator()) {
        it.generatedCodeLine
      }

  private fun getDescriptionToCode(functionSignature: String, enumeratedPromptLines: String) = runBlockingCancellable {
    DescriptionToCodeAssistant.generateCode(
      enumeratedPromptLines,
      functionSignature
    )
      .getOrThrow()
  }

  private fun getEnumeratedPromptLines(prompt: String) =
    prompt.lines().mapIndexed { index, line -> "$index: $line" }.joinToString(System.lineSeparator())
}