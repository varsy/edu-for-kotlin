package com.jetbrains.edu.jarvis.grammar

import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import com.jetbrains.edu.learning.selectedEditor

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
  private val attributes = TextAttributes().apply {
    effectType = EffectType.WAVE_UNDERSCORE
    effectColor = JBColor.RED
  }

  /**
   * Splits the description into sentences and tries to parse them. If the parsing fails,
   * it means that the sentence contains an error and it gets highlighted.
   */
  fun findAndHighlightErrors() =
    getUnparsableSentences().also {
      hasFoundErrors = it.isNotEmpty()
    }.forEach { it.highlight(markupModel, attributes) }

  private fun getUnparsableSentences(): List<UnparsableSentence> {
    val unparsableSentences = mutableListOf<UnparsableSentence>()

    descriptionExpression.prompt.split(DOT)
      .fold(descriptionExpression.promptOffset) { currentOffset, sentence ->
        unparsableSentences.addIfNotNull(getUnparsableSentenceOrNull(sentence, currentOffset))
        currentOffset + sentence.length + 1
      }

    return unparsableSentences
  }

  /**
   * Returns `null` if the sentence is parsed successfully, otherwise returns [UnparsableSentence].
   */
  private fun getUnparsableSentenceOrNull(
    sentence: String,
    sentenceOffset: Int
  ): UnparsableSentence? {
    if (sentence.isBlank()) return null
    if (sentence.matchesGrammar()) return null
    val trimmedLength = sentence.trimStart().length
    val trimmedOffset = sentence.length - trimmedLength

    return UnparsableSentence(
      sentenceOffset + trimmedOffset,
      sentenceOffset + trimmedOffset + sentence.trim().length
    )
  }

  private fun String.matchesGrammar() = try {
    this.parse()
    true
  }
  catch (e: Throwable) {
    // TODO: also check grammar with LLM
    false
  }

  companion object {
    private const val DOT = "."
  }

}
