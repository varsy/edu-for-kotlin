package com.jetbrains.edu.jarvis.actions

import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.jarvis.grammar.GrammarParser
import com.jetbrains.edu.jarvis.grammar.OffsetSentence
import com.jetbrains.edu.jarvis.highlighting.HighlighterManager
import com.jetbrains.edu.jarvis.highlighting.ListenerManager
import com.jetbrains.edu.jarvis.highlighting.descriptiontodraft.DescriptionToDraftHighlighter
import com.jetbrains.edu.jarvis.highlighting.grammar.GrammarHighlighter
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.core.exception.AiAssistantException
import com.jetbrains.educational.ml.cognifire.responses.DescriptionToCodeResponse
import com.jetbrains.educational.ml.cognifire.responses.GeneratedCodeLine
import java.util.concurrent.atomic.AtomicBoolean

/**
 * An action class responsible for handling the running of `description` DSL (Domain-Specific Language) elements.
 * The main task is to parse the `description` DSL, generate code, process the code, and then append a `draft` DSL block with the generated code.
 *
 * @param element The PSI element associated with the `description` DSL that this action is supposed to execute.
 */
class DescriptionExecutorAction(private val element: PsiElement) : AnAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: error("Project was not found")


    // TODO: Update highlighters on PsiElement update
    HighlighterManager.getInstance(project).clearAll()
    ListenerManager.getInstance(project).clearAll()


    val descriptionExpression = DescriptionExpressionParser.parseDescriptionExpression(element, element.language)
    if (descriptionExpression == null) {
      project.notifyError(
        EduJarvisBundle.message("action.not.run.due.to.nested.block.title"),
        EduJarvisBundle.message("action.not.run.due.to.nested.block.text")
      )
      return
    }

    generateCode(project, descriptionExpression)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun generateCode(project: Project, descriptionExpression: DescriptionExpression) = runBackgroundableTask(
    EduJarvisBundle.message("action.progress.bar.message"),
    project
  ) { indicator ->
    if (!CodeGenerationState.getInstance(project).lock()) {
      project.notifyError(content = EduJarvisBundle.message("action.already.running"))
      return@runBackgroundableTask
    }

    ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }

    val unparsableSentences: List<OffsetSentence>

    try {
      unparsableSentences = GrammarParser.getUnparsableSentences(descriptionExpression)
    }
    catch (e: AiAssistantException) {
      project.notifyError(content = EduJarvisBundle.message("action.not.run.due.to.ai.assistant.exception"))
      return@runBackgroundableTask
    }

    GrammarHighlighter.highlightAll(project, unparsableSentences)

    CodeGenerationState.getInstance(project).unlock()

    if (unparsableSentences.isNotEmpty()) {
      project.notifyError(
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.title"),
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.text")
      )
      return@runBackgroundableTask
    }

    // TODO: get the generated code with errors
    val descriptionToCodeTranslation = getDescriptionToCodeTranslation(descriptionExpression)

    val descriptionToDraftLines = descriptionToCodeTranslation
      .groupBy { it.descriptionLineNumber }
      .mapValues { descriptionGroup ->
        descriptionGroup.value.map { it.codeLineNumber }
      }

    val draftToDescriptionLines = descriptionToCodeTranslation
      .groupBy { it.codeLineNumber }
      .mapValues { codeGroup ->
        codeGroup.value.map { it.descriptionLineNumber }
      }

    invokeLater {
      val generatedCode = descriptionToCodeTranslation.joinToString(System.lineSeparator()) { it.generatedCodeLine }
      // TODO: reformat and improve the generated code
      val draftBodyOffset = DraftExpressionWriter.addDraftExpression(project, element, generatedCode, element.language)

      DescriptionToDraftHighlighter(project).setUp(
        descriptionExpression.promptOffset,
        descriptionToDraftLines,
        draftToDescriptionLines,
        draftBodyOffset
      )
    }
  }

  // TODO: Generate code using ML library
  private fun getDescriptionToCodeTranslation(descriptionExpression: DescriptionExpression): DescriptionToCodeResponse {
    return when (descriptionExpression.prompt.filter { !it.isWhitespace() }) {
      """Read user input as an integer and save the result to `userInput`. 
      If `userInput` is divisible by 3,
      Print "Divisible by 3".
      Else,
      Print "Not divisible by 3".
      """.filter { !it.isWhitespace() } -> {
        listOf(
          GeneratedCodeLine(0, 0, "val userInput = readln().toInt()"),
          GeneratedCodeLine(1, 1, "if(userInput % 3 == 0) {"),
          GeneratedCodeLine(2, 2, "  println(\"Divisible by 3\")"),
          GeneratedCodeLine(3, 3, "} else {"),
          GeneratedCodeLine(4, 4, "  println(\"Not divisible by 3\")"),
          GeneratedCodeLine(3, 5, "}")
        )
      }

      """Read user input as an integer and save the result to `userInput`. 
      If `userInput` is divisible by 3,
      Print "Divisible by 3".
      """.filter { !it.isWhitespace() }  -> {
        listOf(
          GeneratedCodeLine(0, 0, "val userInput = readln().toInt()"),
          GeneratedCodeLine(1, 1, "if(userInput % 3 == 0) {"),
          GeneratedCodeLine(2, 2, "  println(\"Divisible by 3\")"),
          GeneratedCodeLine(1, 3, "}"),
        )
      }

      else -> emptyList()
    }
  }

  private fun Project.notifyError(title: String = "", content: String = "") =
    EduNotificationManager.create(
      ERROR, content, title
    ).notify(this)

  @Service(Service.Level.PROJECT)
  private class CodeGenerationState {
    private val isBusy = AtomicBoolean(false)

    fun lock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): CodeGenerationState = project.service()
    }
  }
}
