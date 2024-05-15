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
    val generatedCode = """
      val a = "A"
    """.trimIndent()
    // TODO: reformat and improve the generated code
    DraftExpressionWriter.addDraftExpression(project, element, generatedCode, element.language)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
