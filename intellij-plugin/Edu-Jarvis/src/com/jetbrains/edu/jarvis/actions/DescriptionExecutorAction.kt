package com.jetbrains.edu.jarvis.actions

import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.markup.*
import com.intellij.psi.PsiElement
import com.intellij.ui.JBColor
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.jarvis.grammar.parse
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import com.jetbrains.edu.learning.notification.EduNotificationManager


/**
 * An action class responsible for handling the running of `description` DSL (Domain-Specific Language) elements.
 * The main task is to parse the `description` DSL, generate code, process the code, and then append a `draft` DSL block with the generated code.
 *
 * @param element The PSI element associated with the `description` DSL that this action is supposed to execute.
 */
class DescriptionExecutorAction(private val element: PsiElement) : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: error("Project was not found")

    val descriptionExpression = DescriptionExpressionParser.parseDescriptionExpression(element, element.language)
    if (descriptionExpression == null) {
      EduNotificationManager.create(
        ERROR,
        EduJarvisBundle.message("action.not.run.due.to.nested.block.title"),
        EduJarvisBundle.message("action.not.run.due.to.nested.block.text")
      )
        .notify(project)
      return
    }

    val markupModel = e.getData(PlatformDataKeys.EDITOR)?.markupModel ?: error("Editor was not found")
    markupModel.removeAllHighlighters()

    parseDescription(descriptionExpression, markupModel)

    if (markupModel.allHighlighters.isNotEmpty()) {
      EduNotificationManager.create(
        ERROR,
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.title"),
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.text")
      )
        .notify(project)
      return
    }

    // TODO: get the generated code with errors
    val generatedCode = descriptionExpression.codeBlock
    // TODO: reformat and improve the generated code
    DraftExpressionWriter.addDraftExpression(project, element, generatedCode, element.language)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  /**
   * Parses the description block by splitting it into sentences and highlights those that do not match the grammar.
   */
  private fun parseDescription(descriptionExpression: DescriptionExpression, markupModel: MarkupModel) {
    val attributes = TextAttributes()
    attributes.effectColor = JBColor.RED
    attributes.effectType = EffectType.LINE_UNDERSCORE

    descriptionExpression.prompt.split(DOT)
      .fold(descriptionExpression.promptOffset) { currentOffset, sentence ->
        processSentence(sentence, currentOffset, markupModel, attributes)
        currentOffset + sentence.length + 1
      }
  }

  /**
   * Processes the sentence and determines whether to highlight the sentence or not.
   */
  private fun processSentence(
    sentence: String,
    sentenceOffset: Int,
    markupModel: MarkupModel,
    attributes: TextAttributes
  ) {
    if (sentence.isBlank()) return
    if (sentence.matchesGrammar()) return
    val trimmedLength = sentence.trimStart().length
    val trimmedOffset = sentence.length - trimmedLength

    markupModel.addRangeHighlighter(
      sentenceOffset + trimmedOffset,
      sentenceOffset + trimmedOffset + sentence.trim().length,
      HighlighterLayer.ERROR, attributes, HighlighterTargetArea.EXACT_RANGE
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
    const val DOT = '.'
  }

}
