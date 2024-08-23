package com.jetbrains.edu.jarvis.actions

import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.jarvis.GeneratedCodeParser
import com.jetbrains.edu.jarvis.codegeneration.CodeGenerationState
import com.jetbrains.edu.jarvis.codegeneration.CodeGenerator
import com.jetbrains.edu.jarvis.grammar.GrammarParser
import com.jetbrains.edu.jarvis.grammar.OffsetSentence
import com.jetbrains.edu.jarvis.highlighting.HighlighterManager
import com.jetbrains.edu.jarvis.highlighting.ListenerManager
import com.jetbrains.edu.jarvis.highlighting.descriptiontocode.DescriptionToCodeHighlighter
import com.jetbrains.edu.jarvis.highlighting.grammar.GrammarHighlighter
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.jarvis.models.DescriptionExpression
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.tasks.cognifire.PromptCodeState
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.ml.core.exception.AiAssistantException

/**
 * An action class responsible for handling the running of `description` DSL (Domain-Specific Language) elements.
 * The main task is to parse the `description` DSL, generate code, process the code, and then append a `draft` DSL block with the generated code.
 *
 * @param element The PSI element associated with the `description` DSL that this action is supposed to execute.
 */
class DescriptionExecutorAction(private val element: PsiElement, private val id: String) : AnAction() {
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
    runLocked(project) {
      runWithProgressBar(indicator) {
        val unparsableSentences = checkGrammar(descriptionExpression, project)
        GrammarHighlighter.highlightAll(project, unparsableSentences)
        handleCodeGeneration(project, descriptionExpression)
      }
    }
  }

  private fun runLocked(project: Project, execution: () -> Unit) {
    val codeGenerationState = CodeGenerationState.getInstance(project)

    if (!codeGenerationState.lock()) {
      project.notifyError(content = EduJarvisBundle.message("action.already.running"))
      return
    }
    try {
      execution()
    } catch (e: AiAssistantException) {
      project.notifyError(title = EduJarvisBundle.message("action.not.run.due.to.ai.assistant.exception"), content = e.message)
    } catch (e: Throwable) {
      CodeGenerationState.getInstance(project).unlock()
      project.notifyError(content = EduJarvisBundle.message("action.not.run.due.to.unknown.exception"))
    }
    finally {
      CodeGenerationState.getInstance(project).unlock()
    }
  }

  private fun runWithProgressBar(indicator: ProgressIndicator, execution: () -> Unit) {
    ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }
    execution()
  }

  private fun checkGrammar(descriptionExpression: DescriptionExpression, project: Project): List<OffsetSentence> {
    val unparsableSentences = GrammarParser.getUnparsableSentences(descriptionExpression)

    if (unparsableSentences.isNotEmpty()) {
      project.notifyError(
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.title"),
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.text")
      )
      project.getCurrentTask()?.let {
        it.promptActions.updateAction(id, PromptCodeState.CodeFailed)
        TaskToolWindowView.getInstance(project).updateCheckPanel(it)
      }
    }

    return unparsableSentences
  }

  private fun handleCodeGeneration(
    project: Project,
    descriptionExpression: DescriptionExpression,
  ) {
    val codeGenerator = CodeGenerator(descriptionExpression)

    invokeLater {
      val generatedCode = codeGenerator.generatedCode
      val draftExpression = DraftExpressionWriter.addDraftExpression(
        project,
        element,
        generatedCode,
        element.language
      )
      DescriptionToCodeHighlighter(project).setUp(
        descriptionExpression,
        draftExpression,
        codeGenerator.descriptionToCodeLines,
        codeGenerator.codeToDescriptionLines
      )

      val state = if (GeneratedCodeParser.hasErrors(project, generatedCode, element.language)) {
        PromptCodeState.CodeFailed
      } else {
        PromptCodeState.CodeSuccess
      }
      project.getCurrentTask()?.let {
        it.promptActions.updateAction(id, state)
        TaskToolWindowView.getInstance(project).updateCheckPanel(it)
      }
    }
  }

  private fun Project.notifyError(title: String = "", content: String) =
    EduNotificationManager.create(
      ERROR, content, title
    ).notify(this)

}
