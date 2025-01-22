package com.jetbrains.edu.cognifire.grammar

import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.educational.ml.cognifire.core.GrammarCheckerAssistant
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse.GeneratedCodeLine

/**
 * Parses provided Prompt.
 */
object GrammarParser {
  private const val TODO_MARKER = "TODO"

  /**
   * Identifies unparsable lines from the provided prompt and code content.
   *
   * @param promptToCode The content mapping between prompt and generated code lines to be analyzed.
   * @param promptLineOffset The offset to be applied to prompt line numbers during processing.
   * @param codeLineOffset The offset to be applied to generated code line numbers during processing.
   * @return An instance of UnparsableSentenceLink containing sets of unparsable prompt and code line numbers.
   */
  fun getUnparsableProdeLines(
    promptToCode: PromptToCodeContent,
    promptLineOffset: Int,
    codeLineOffset: Int
  ): UnparsableSentenceLink = runBlockingCancellable {
    val unparsableFromPrompt = findUnparsableLinesFromPrompt(promptToCode, promptLineOffset, codeLineOffset)
    val unparsableFromCode = findUnparsableLinesFromCode(promptToCode, promptLineOffset, codeLineOffset)

    unparsableFromPrompt + unparsableFromCode
  }

  private suspend fun findUnparsableLinesFromPrompt(
    promptToCode: PromptToCodeContent,
    promptLineOffset: Int,
    codeLineOffset: Int
  ): UnparsableSentenceLink {
    val unparsablePromptLines = promptToCode
      .distinctBy { it.promptLineNumber }
      .filterNot { it.promptLine.matchesGrammarStatic() }
      .filterGrammarMl()
      .map { it.promptLineNumber + promptLineOffset }
      .toSet()

    val unparsableCodeLines = promptToCode
      .filter { it.promptLineNumber + promptLineOffset in unparsablePromptLines }
      .map { it.codeLineNumber + codeLineOffset }
      .toSet()

    return UnparsableSentenceLink(unparsablePromptLines, unparsableCodeLines)
  }

  private fun findUnparsableLinesFromCode(
    promptToCode: PromptToCodeContent,
    promptLineOffset: Int,
    codeLineOffset: Int
  ): UnparsableSentenceLink {
    val unparsableCodeLines = promptToCode
      .distinctBy { it.codeLineNumber }
      .filter { it.generatedCodeLine.startsWith(TODO_MARKER) }
      .map { it.codeLineNumber + codeLineOffset }
      .toSet()

    val unparsablePromptLines = promptToCode
      .filter { it.codeLineNumber + codeLineOffset in unparsableCodeLines }
      .map { it.promptLineNumber + promptLineOffset }
      .toSet()

    return UnparsableSentenceLink(unparsablePromptLines, unparsableCodeLines)
  }
  
  private fun String.matchesGrammarStatic() = runCatching {
    parse()
    true
  }.getOrElse { false }

  private suspend fun Collection<GeneratedCodeLine>.filterGrammarMl(): List<GeneratedCodeLine> {
    val mask = GrammarCheckerAssistant.checkGrammar(map { it.promptLine }).getOrThrow().map { it.not() }
    return filterByMask(mask)
  }

  private fun <E> Collection<E>.filterByMask(mask: List<Boolean>): List<E> = filterIndexed { index, _ -> mask[index] }

}
