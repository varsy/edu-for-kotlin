package com.jetbrains.edu.jarvis.actions

import com.jetbrains.edu.jarvis.codegeneration.CodeGenerationState
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.jarvis.codegeneration.CodeGenerator
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
    executeAction(project, descriptionExpression)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  private fun executeAction(project: Project, descriptionExpression: DescriptionExpression) = runBackgroundableTask(
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
    catch (e: Throwable) {
      project.notifyError(content = EduJarvisBundle.message("action.not.run.due.to.unknown.exception"))
      return@runBackgroundableTask
    }

    GrammarHighlighter.highlightAll(project, unparsableSentences)

    if (unparsableSentences.isNotEmpty()) {
      CodeGenerationState.getInstance(project).unlock()
      project.notifyError(
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.title"),
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.text")
      )
      return@runBackgroundableTask
    }


    CodeGenerationState.getInstance(project).unlock()
    val codeGenerator = CodeGenerator(descriptionExpression)

    invokeLater {
      val generatedCode = codeGenerator.generatedCode
      // TODO: reformat and improve the generated code
      val draftBodyOffset = DraftExpressionWriter.addDraftExpression(project, element, generatedCode, element.language)

      DescriptionToDraftHighlighter(project).setUp(
        descriptionExpression.promptOffset,
        draftBodyOffset,
        codeGenerator.descriptionToDraftLines,
        codeGenerator.draftToDescriptionLines
      )
    }
  }

  private fun Project.notifyError(title: String = "", content: String) =
    EduNotificationManager.create(
      ERROR, content, title
    ).notify(this)

}
