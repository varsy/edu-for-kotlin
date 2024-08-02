package com.jetbrains.edu.jarvis.grammar

import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jarvis.highlighting.HighlighterManager
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import com.jetbrains.educational.ml.jarvis.core.GrammarCheckerAssistant

/**
 * Parses the provided description.
 * @param project The project in which the currently edited file is located.
 * @param descriptionExpression The description to be parsed.
 */
class GrammarParser(private val project: Project, private val descriptionExpression: DescriptionExpression) {

  private val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)

  /**
   * Splits the description into sentences and tries to parse them. If the parsing fails,
   * it means that the sentence contains an error and it gets highlighted.
   */
  fun findAndHighlightErrors() {
    getUnparsableSentences().forEach {
      HighlighterManager
        .getInstance(project)
        .addGrammarHighlighter(it.startOffset, it.endOffset, attributes)
    }
  }

  private fun getUnparsableSentences(): List<OffsetSentence> {
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
