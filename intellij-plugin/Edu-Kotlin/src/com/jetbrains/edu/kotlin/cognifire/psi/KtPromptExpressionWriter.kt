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
  override fun addExpression(project: Project, element: PsiElement, text: String, oldExpression: PromptExpression?): PromptExpression? {
    if (element !is KtCallExpression) return null
    val promptPromptPsi = element.valueArguments.firstOrNull() ?: return null
    if (oldExpression == null) return null
    val prompt = "\"\"\"${System.lineSeparator()}$text${System.lineSeparator()}\"\"\""
    val newValueArgument = KtPsiFactory(project).createArgument(prompt)
    val documentManager = PsiDocumentManager.getInstance(project)

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      documentManager.commitAllDocuments()
      promptPromptPsi.replace(newValueArgument)
    })

    val expressionElement = element
    val contentElement = element.valueArguments.firstOrNull()?.getArgumentExpression() ?: return null

    return PromptExpression(
      SmartPointerManager.createPointer(expressionElement),
      SmartPointerManager.createPointer(contentElement),
      oldExpression.functionSignature,
      text,
      oldExpression.code
    )
  }
}