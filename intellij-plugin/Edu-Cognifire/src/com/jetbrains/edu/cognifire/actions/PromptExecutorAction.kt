package com.jetbrains.edu.cognifire.actions

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
import com.jetbrains.edu.cognifire.PromptExpressionParser
import com.jetbrains.edu.cognifire.CodeExpressionWriter
import com.jetbrains.edu.cognifire.GeneratedCodeParser
import com.jetbrains.edu.cognifire.codegeneration.CodeGenerationState
import com.jetbrains.edu.cognifire.codegeneration.CodeGenerator
import com.jetbrains.edu.cognifire.grammar.GrammarParser
import com.jetbrains.edu.cognifire.grammar.OffsetSentence
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.cognifire.highlighting.ListenerManager
import com.jetbrains.edu.cognifire.highlighting.prompttocode.PromptToCodeHighlighter
import com.jetbrains.edu.cognifire.highlighting.grammar.GrammarHighlighter
import com.jetbrains.edu.cognifire.messages.EduCognifireBundle
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.tasks.cognifire.PromptCodeState
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.ml.core.exception.AiAssistantException

/**
 * An action class responsible for handling the running of `prompt` DSL (Domain-Specific Language) elements.
 * The main task is to parse the `prompt` DSL, generate code, process the code, and then append a `code` DSL block with the generated code.
 *
 * @param element The PSI element associated with the `prompt` DSL that this action is supposed to execute.
 */
class PromptExecutorAction(private val element: PsiElement, private val id: String) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: error("Project was not found")


    // TODO: Update highlighters on PsiElement update
    HighlighterManager.getInstance(project).clearAll()
    ListenerManager.getInstance(project).clearAll()

    val promptExpression = PromptExpressionParser.parsePromptExpression(element, element.language)
    if (promptExpression == null) {
      project.notifyError(
        EduCognifireBundle.message("action.not.run.due.to.nested.block.title"),
        EduCognifireBundle.message("action.not.run.due.to.nested.block.text")
      )
      return
    }
    executeAction(project, promptExpression)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  private fun executeAction(project: Project, promptExpression: PromptExpression) = runBackgroundableTask(
    EduCognifireBundle.message("action.progress.bar.message"),
    project
  ) { indicator ->
    runLocked(project) {
      runWithProgressBar(indicator) {
        val unparsableSentences = checkGrammar(promptExpression, project)
        GrammarHighlighter.highlightAll(project, unparsableSentences)
        handleCodeGeneration(project, promptExpression)
      }
    }
  }

  private fun runLocked(project: Project, execution: () -> Unit) {
    val codeGenerationState = CodeGenerationState.getInstance(project)

    if (!codeGenerationState.lock()) {
      project.notifyError(content = EduCognifireBundle.message("action.already.running"))
      return
    }
    try {
      execution()
    } catch (e: AiAssistantException) {
      project.notifyError(title = EduCognifireBundle.message("action.not.run.due.to.ai.assistant.exception"), content = e.message)
    } catch (e: Throwable) {
      CodeGenerationState.getInstance(project).unlock()
      project.notifyError(content = EduCognifireBundle.message("action.not.run.due.to.unknown.exception"))
    }
    finally {
      CodeGenerationState.getInstance(project).unlock()
    }
  }

  private fun runWithProgressBar(indicator: ProgressIndicator, execution: () -> Unit) {
    ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }
    execution()
  }

  private fun checkGrammar(promptExpression: PromptExpression, project: Project): List<OffsetSentence> {
    val unparsableSentences = GrammarParser.getUnparsableSentences(promptExpression)

    if (unparsableSentences.isNotEmpty()) {
      project.notifyError(
        EduCognifireBundle.message("action.not.run.due.to.incorrect.grammar.title"),
        EduCognifireBundle.message("action.not.run.due.to.incorrect.grammar.text")
      )
    }

    return unparsableSentences
  }

  private fun handleCodeGeneration(
    project: Project,
    promptExpression: PromptExpression,
  ) {
    val codeGenerator = CodeGenerator(promptExpression)

    invokeLater {
      val generatedCode = codeGenerator.generatedCode
      val codeExpression = CodeExpressionWriter.addCodeExpression(
        project,
        element,
        generatedCode,
        element.language
      )
      PromptToCodeHighlighter(project).setUp(
        promptExpression,
        codeExpression,
        codeGenerator.promptToCodeLines,
        codeGenerator.codeToPromptLines
      )

      val state = if (GeneratedCodeParser.hasErrors(project, generatedCode, element.language)) {
        PromptCodeState.CodeFailed
      } else {
        PromptCodeState.CodeSuccess
      }
      project.getCurrentTask()?.let {
        it.promptActionManager.updateAction(id, state)
        TaskToolWindowView.getInstance(project).updateCheckPanel(it)
      }
    }
  }

  private fun Project.notifyError(title: String = "", content: String) =
    EduNotificationManager.create(
      ERROR, content, title
    ).notify(this)

}
