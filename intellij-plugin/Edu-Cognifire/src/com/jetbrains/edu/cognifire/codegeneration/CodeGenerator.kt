package com.jetbrains.edu.cognifire.codegeneration

import com.intellij.lang.Language
import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.cognifire.utils.RedundantTodoCleaner
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.utils.FunctionNamePlaceHolderSubstitutor
import com.jetbrains.edu.cognifire.utils.toGeneratedCode
import com.jetbrains.edu.cognifire.utils.toPrompt
import com.jetbrains.educational.ml.cognifire.core.PromptSyncAssistant
import com.jetbrains.educational.ml.cognifire.core.PromptToCodeAssistant
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

class CodeGenerator(
  private val promptExpression: PromptExpression,
  private val project: Project,
  private val language: Language,
  private val previousPromptToCode: PromptToCodeContent?,
  private val codeExpression: CodeExpression?
) {

  val finalPromptToCodeTranslation = improvePromptToCode()

  val promptToCodeLines = finalPromptToCodeTranslation
    .groupBy { it.promptLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.codeLineNumber }
    }
  val codeToPromptLines = finalPromptToCodeTranslation
    .groupBy { it.codeLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.promptLineNumber }
    }
  val generatedCode = finalPromptToCodeTranslation.toGeneratedCode()
  val generatedPrompt = finalPromptToCodeTranslation.toPrompt()

  private fun generatePromptToCode() =
    if (previousPromptToCode != null && codeExpression?.code != null && previousPromptToCode.toGeneratedCode() != codeExpression.code
                          && previousPromptToCode.toPrompt().filter { !it.isWhitespace() } == promptExpression.prompt.filter { !it.isWhitespace() }) {
      syncPrompt(
        previousPromptToCode,
        codeExpression.code.lines().enumerate(0),
        getSetOfModifiedCodeLines(previousPromptToCode.toGeneratedCode(), codeExpression.code),
        promptExpression.functionSignature.toStringWithPlaceHolder()
      )
    } else if (previousPromptToCode != null && codeExpression?.code != null && previousPromptToCode.toGeneratedCode() != codeExpression.code
             && previousPromptToCode.toPrompt() != promptExpression.prompt) {
      getCodeFromPrompt(promptExpression.functionSignature.toStringWithPlaceHolder(), getEnumeratedPromptLines(promptExpression)) // TODO: handle conflict
    } else {
      getCodeFromPrompt(promptExpression.functionSignature.toStringWithPlaceHolder(), getEnumeratedPromptLines(promptExpression))
    }

  private fun improvePromptToCode(): PromptToCodeContent {
    val promptToCodeSubstitutedFunctionName =
      FunctionNamePlaceHolderSubstitutor.substitutePlaceHolders(generatePromptToCode().content, promptExpression.functionSignature.getNonFqName())
    val promptToCodeClearedFromWrongTodos =
      RedundantTodoCleaner.deleteWrongTodo(promptToCodeSubstitutedFunctionName, promptExpression.functionSignature)
    return InspectionProcessor.applyInspections(
      promptToCodeClearedFromWrongTodos,
      promptExpression.functionSignature.toString(),
      project,
      language
    ) ?: promptToCodeClearedFromWrongTodos
  }

  private fun getCodeFromPrompt(functionSignature: String, enumeratedPromptLines: String) = runBlockingCancellable {
    PromptToCodeAssistant.generateCode(
      enumeratedPromptLines,
      functionSignature
    )
      .getOrThrow()
  }

  private fun syncPrompt(previousPromptToCode: PromptToCodeContent, modifiedCode: String, modifiedCodeLineNumbers: Set<Int>, functionSignature: String) =
    runBlockingCancellable {
      PromptSyncAssistant.syncPrompt(previousPromptToCode, modifiedCode, modifiedCodeLineNumbers, functionSignature).getOrThrow()
    }

  private fun getSetOfModifiedCodeLines(code: String, modifiedCode: String): Set<Int> {
    val oldCodeLines = code.lines()
    val modifiedCodeLines = modifiedCode.lines()
    val modifiedCodeLineNumbers = mutableSetOf<Int>()
    for (i in 0 until maxOf(oldCodeLines.size, modifiedCodeLines.size)) {
      if (oldCodeLines.getOrNull(i)?.trim() != modifiedCodeLines.getOrNull(i)?.trim()) {
        modifiedCodeLineNumbers.add(i)
      }
    }
    return modifiedCodeLineNumbers
  }

  private fun getEnumeratedPromptLines(promptExpression: PromptExpression): String {
    val promptLines = promptExpression.prompt.lines().filter { it.isNotBlank() }
    val codeLines = promptExpression.code.lines().filter { it.isNotBlank() }

    return promptLines.enumerate(0) + System.lineSeparator() +
           codeLines.enumerate(promptLines.size)
  }

  private fun List<String>.enumerate(startIndex: Int): String {
    return mapIndexed { index, line -> "${index + startIndex}: $line" }
      .joinToString(System.lineSeparator())
  }
}