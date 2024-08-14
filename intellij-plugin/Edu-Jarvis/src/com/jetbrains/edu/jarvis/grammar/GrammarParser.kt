package com.jetbrains.edu.jarvis.grammar

import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.jarvis.models.DescriptionExpression
import com.jetbrains.educational.ml.cognifire.core.GrammarCheckerAssistant

private const val DOT = "."

/**
 * Parses the provided description.
 */
object GrammarParser {

  /**
   * Returns a list of OffsetSentences that are unparsable from the given DescriptionExpression.
   *
   * @param descriptionExpression The DescriptionExpression containing the promptOffset and prompt.
   * @return The list of unparsable OffsetSentences.
   */
  fun getUnparsableSentences(descriptionExpression: DescriptionExpression): List<OffsetSentence> {
    val sentences = mutableListOf<OffsetSentence>()

    descriptionExpression.prompt.split(DOT)
      .fold(descriptionExpression.promptOffset) { currentOffset, sentence ->
        sentences.add(OffsetSentence(sentence, currentOffset))
        currentOffset + sentence.length + 1
      }

    return runBlockingCancellable {
      sentences.filter { it.sentence.isNotBlank() }.filterGrammarStatic().filterGrammarMl()
    }
  }

  private fun String.matchesGrammarStatic() = try {
    parse()
    true
  } catch (e: Throwable) {
    false
  }

  private fun List<OffsetSentence>.filterGrammarStatic() = filter { !it.sentence.matchesGrammarStatic() }
  
  private suspend fun List<OffsetSentence>.filterGrammarMl(): List<OffsetSentence> {
      val mask = GrammarCheckerAssistant.checkGrammar(
        map { it.sentence }
      ).getOrThrow().map { it.not() }
      return filterByMask(mask)
  }

  private fun <E> List<E>.filterByMask(mask: List<Boolean>): List<E>
    = filterIndexed { index, _ -> mask[index] }

}
