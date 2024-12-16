package com.jetbrains.edu.cognifire.actions

import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.codegeneration.CodeGenerationState
import com.jetbrains.edu.cognifire.codegeneration.CodeGenerator
import com.jetbrains.edu.cognifire.grammar.GrammarParser
import com.jetbrains.edu.cognifire.grammar.OffsetSentence
import com.jetbrains.edu.cognifire.highlighting.GuardedBlockManager
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.cognifire.highlighting.ListenerManager
import com.jetbrains.edu.cognifire.highlighting.grammar.GrammarHighlighterProcessor
import com.jetbrains.edu.cognifire.highlighting.prompttocode.PromptToCodeHighlighter
import com.jetbrains.edu.cognifire.log.Logger
import com.jetbrains.edu.cognifire.manager.PromptActionManager
import com.jetbrains.edu.cognifire.manager.PromptCodeState
import com.jetbrains.edu.cognifire.messages.EduCognifireBundle
import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.models.ProdeExpression
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.parsers.CodeExpressionParser
import com.jetbrains.edu.cognifire.parsers.GeneratedCodeParser
import com.jetbrains.edu.cognifire.parsers.PromptExpressionParser
import com.jetbrains.edu.cognifire.writers.CodeExpressionWriter
import com.jetbrains.edu.cognifire.writers.PromptExpressionWriter
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.ml.core.exception.AiAssistantException

/**
 * An action class responsible for handling the running of `prompt` DSL (Domain-Specific Language) elements.
 * The main task is to parse the `prompt` DSL, generate code, process the code, and then append a `code` DSL block with the generated code.
 *
 * @param element The PSI element associated with the `prompt` DSL that this action is supposed to execute.
 * @param prodeId A unique identifier for the Prompt-to-Code.
 * @param task The educational task within which this action is being executed.
 */
class PromptExecutorAction(private val element: PsiElement, private val prodeId: String, private val task: Task) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: error("Project was not found")
    val document = getDocument() ?: return

    // TODO: Update highlighters on PsiElement update
    HighlighterManager.getInstance().clearAll(prodeId)
    ListenerManager.getInstance(project).clearAll(prodeId)
    GuardedBlockManager.getInstance().removeGuardedBlock(prodeId, document)
    document.setReadOnly(true)

    val promptExpression = PromptExpressionParser.parsePromptExpression(element, element.language)
    if (promptExpression == null) {
      project.notifyError(
        EduCognifireBundle.message("action.not.run.due.to.nested.block.title"),
        EduCognifireBundle.message("action.not.run.due.to.nested.block.text")
      )
      getDocument()?.setReadOnly(false)
      return
    }
    val codeExpression = CodeExpressionParser.getCodeExpression(element, element.language)
    executeAction(project, promptExpression, codeExpression)
  }

  private fun getDocument() = runReadAction { FileDocumentManager.getInstance().getDocument(element.containingFile.virtualFile) }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
  private fun executeAction(project: Project, promptExpression: PromptExpression, codeExpression: CodeExpression?) = runBackgroundableTask(
    EduCognifireBundle.message("action.progress.bar.message"),
    project
  ) { indicator ->
    runLocked(project) {
      runWithProgressBar(indicator) {
        handleCodeGeneration(project, promptExpression, codeExpression)
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
    }
    catch (e: AiAssistantException) {
      project.notifyError(title = EduCognifireBundle.message("action.not.run.due.to.ai.assistant.exception"), content = e.message)
    }
    catch (_: Throwable) {
      CodeGenerationState.getInstance(project).unlock()
      project.notifyError(content = EduCognifireBundle.message("action.not.run.due.to.unknown.exception"))
    } finally {
      CodeGenerationState.getInstance(project).unlock()
      getDocument()?.setReadOnly(false)
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
        EduCognifireBundle.message("action.not.run.due.to.incorrect.grammar.text"),
        promptExpression
      )
    }

    return unparsableSentences
  }

  private fun handleCodeGeneration(
    project: Project,
    promptExpression: PromptExpression,
    codeExpression: CodeExpression?
  ) {
    val promptActionManager = PromptActionManager.getInstance(project)
    val codeGenerator =
      CodeGenerator(promptExpression, project, element.language, promptActionManager.getAction(prodeId)?.promptToCode, codeExpression)

    invokeLater {
      val generatedCode = codeGenerator.generatedCode
      val (newPromptExpression, newCodeExpression) = syncProde(project, codeGenerator, promptExpression)
      PromptToCodeHighlighter(project, prodeId).setUp(
        newPromptExpression,
        newCodeExpression,
        codeGenerator.promptToCodeLines,
        codeGenerator.codeToPromptLines
      )

      val state = if (GeneratedCodeParser.hasErrors(project, generatedCode, newPromptExpression.functionSignature, element.language)) {
        PromptCodeState.CodeFailed
      }
      else {
        PromptCodeState.CodeSuccess
      }
      var unparsableSentences = emptyList<OffsetSentence>()
      if (state == PromptCodeState.CodeFailed) {
        unparsableSentences = checkGrammar(newPromptExpression, project)
        GrammarHighlighterProcessor.highlightAll(project, unparsableSentences, prodeId)
      }
      Logger.cognifireLogger.info(
        """Lesson id: ${task.lesson.id}    Task id: ${task.id}    Action id: $prodeId
           | Text prompt: ${newPromptExpression.prompt}
           | Code prompt: ${newPromptExpression.code}
           | Generated code: $generatedCode
           | Has TODO blocks: ${state == PromptCodeState.CodeFailed}
           | Has unparsable sentences - ${unparsableSentences.isNotEmpty()}: ${unparsableSentences.map { it.sentence }}
        """.trimMargin()
      )
      promptActionManager.updateAction(prodeId, state, codeGenerator.finalPromptToCodeTranslation)
      project.getCurrentTask()?.let {
        it.isPromptActionsGeneratedSuccessfully = promptActionManager.generatedSuccessfully(task.id)
        TaskToolWindowView.getInstance(project).updateCheckPanel(it)
      }
    }
  }

  private fun syncProde(project: Project, codeGenerator: CodeGenerator, promptExpression: PromptExpression): ProdeExpression {
    val newPromptExpression = PromptExpressionWriter.addPromptExpression(
      project,
      element,
      codeGenerator.generatedPrompt,
      promptExpression,
      element.language
    )
    val codeExpression = CodeExpressionWriter.addCodeExpression(
      project,
      element,
      codeGenerator.generatedCode,
      element.language
    )
    return ProdeExpression(newPromptExpression, codeExpression)
  }

  private fun Project.notifyError(title: String = "", content: String, promptExpression: PromptExpression? = null) =
    EduNotificationManager.create(
      type = ERROR,
      title = title,
      content = content
    ).notify(this).also {
      promptExpression?.let {
        Logger.cognifireLogger.info(
          """Lesson id: ${task.lesson.id}    Task id: ${task.id}    Action id: $prodeId
           | Error: $title
           | ErrorMessage: $content
           | Text prompt: ${it.prompt}
           | Code prompt: ${it.code}
        """.trimMargin()
        )
      } ?: run {
        Logger.cognifireLogger.info(
          """Lesson id: ${task.lesson.id}    Task id: ${task.id}    Action id: $prodeId
           | Error: $title
           | ErrorMessage: $content
        """.trimMargin()
        )
      }
    }

}
