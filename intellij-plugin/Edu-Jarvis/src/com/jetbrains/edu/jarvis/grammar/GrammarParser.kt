package com.jetbrains.edu.jarvis.grammar

import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import com.jetbrains.educational.ml.cognifire.core.GrammarCheckerAssistant

/**
 * Parses the provided description.
 * @param project The project in which the currently edited file is located.
 * @param descriptionExpression The description to be parsed.
 */
class GrammarParser(private val project: Project, private val descriptionExpression: DescriptionExpression) {

  /**
   * Retrieves a list of unparsable sentences from the description.
   * Each sentence is represented by an OffsetSentence object that includes the sentence
   * and its offset in the file.
   *
   * @return The list of unparsable sentences.
   */
  fun getUnparsableSentences(): List<OffsetSentence> {
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

  companion object {
    private const val DOT = "."
  }

}
