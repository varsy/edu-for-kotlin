package com.jetbrains.edu.jarvis.actions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils


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
      Notification(
        MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
        EduJarvisBundle.message("action.not.run.due.to.nested.block.text"),
        NotificationType.ERROR
      ).notify(project)
      return
    }

    // TODO: get the generated code with errors
    val generatedCode = descriptionExpression.codeBlock
    // TODO: reformat and improve the generated code
    DraftExpressionWriter.addDraftExpression(project, element, generatedCode, element.language)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
