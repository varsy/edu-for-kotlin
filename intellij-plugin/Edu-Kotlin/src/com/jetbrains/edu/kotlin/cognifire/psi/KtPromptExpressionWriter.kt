package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.writers.PromptExpressionWriter
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

class KtPromptExpressionWriter : PromptExpressionWriter {
  override fun addExpression(
    project: Project,
    element: PsiElement,
    text: String,
    oldExpression: PromptExpression?
  ): PromptExpression? {
    if (element !is KtCallExpression || oldExpression == null) return null

    val firstArgument = element.valueArguments.firstOrNull() ?: return null

    val formattedPrompt = buildString {
      append("\"\"\"\n")
      append(text)
      append("\n\"\"\"")
    }

    val newValueArgument = KtPsiFactory(project).createArgument(formattedPrompt)
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      PsiDocumentManager.getInstance(project).commitAllDocuments()
      firstArgument.replace(newValueArgument)
    })

    val contentElement = element.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null

    return PromptExpression(
      SmartPointerManager.createPointer(element),
      SmartPointerManager.createPointer(contentElement),
      oldExpression.functionSignature,
      text,
      oldExpression.code
    )
  }
}