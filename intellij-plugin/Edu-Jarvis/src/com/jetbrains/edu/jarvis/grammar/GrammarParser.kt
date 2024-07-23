package com.jetbrains.edu.jarvis.grammar

import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import com.jetbrains.edu.learning.selectedEditor
import com.jetbrains.educational.ml.jarvis.core.DescriptionGrammarChecker

/**
 * Parses the provided description.
 * @param project The project in which the currently edited file is located.
 * @param descriptionExpression The description to be parsed.
 */
class GrammarParser(project: Project, private val descriptionExpression: DescriptionExpression) {

  var hasFoundErrors = false
    private set

  private val markupModel = project.selectedEditor?.markupModel?.also {
    // TODO: Remove only the highlighters created by this action
    it.removeAllHighlighters()
  }
  private val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(CodeInsightColors.WARNINGS_ATTRIBUTES)

  /**
   * Splits the description into sentences and tries to parse them. If the parsing fails,
   * it means that the sentence contains an error and it gets highlighted.
   */
  fun findAndHighlightErrors() =
    getUnparsableSentences().also {
      hasFoundErrors = it.isNotEmpty()
    }.forEach { it.highlight(markupModel, attributes) }

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
    val mask = DescriptionGrammarChecker.checkGrammar(
      map { it.sentence }
    ).toList()
    return filterByMask(mask, true)
  }

  private fun <E> List<E>.filterByMask(mask: List<Boolean>, inverse: Boolean = false): List<E>
    = filterIndexed { index, _ -> mask[index] xor inverse }

  companion object {
    private const val DOT = "."
  }

}
