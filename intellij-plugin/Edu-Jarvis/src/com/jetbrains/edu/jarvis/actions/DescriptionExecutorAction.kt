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
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import com.jetbrains.edu.learning.notification.EduNotificationManager
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

    generateCode(project, descriptionExpression)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun generateCode(project: Project, descriptionExpression: DescriptionExpression) = runBackgroundableTask(
    EduJarvisBundle.message("action.progress.bar.message"),
    project
  ) { indicator ->
    if (!CodeGenerationState.getInstance(project).lock()) {
      EduNotificationManager.create(
        ERROR,
        content = EduJarvisBundle.message("action.already.running"),
      ).notify(project)
      return@runBackgroundableTask
    }

    ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }

    val grammarParser = GrammarParser(project, descriptionExpression)
    grammarParser.findAndHighlightErrors()

    CodeGenerationState.getInstance(project).unlock()

    if (grammarParser.hasFoundErrors) {
      EduNotificationManager.create(
        ERROR,
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.title"),
        EduJarvisBundle.message("action.not.run.due.to.incorrect.grammar.text")
      ).notify(project)
      return@runBackgroundableTask
    }

    invokeLater {
      // TODO: get the generated code with errors
      val generatedCode = descriptionExpression.codeBlock
      // TODO: reformat and improve the generated code
      DraftExpressionWriter.addDraftExpression(project, element, generatedCode, element.language)
    }
  }

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
