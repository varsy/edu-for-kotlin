package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.writers.PromptExpressionWriter
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.utils.isPromptBlock
import com.jetbrains.edu.kotlin.cognifire.utils.getBaseContentOffset
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class KtPromptExpressionWriter : PromptExpressionWriter {
  override fun addExpression(project: Project, element: PsiElement, text: String, oldExpression: PromptExpression?): PromptExpression? {
    if (!element.isPromptBlock() || element !is KtCallExpression) return null
    val promptPromptPsi = element.valueArguments.firstOrNull() ?: return null
    if (oldExpression == null) return null
    val prompt = "\"\"\"${System.lineSeparator()}$text${System.lineSeparator()}\"\"\""
    val newValueArgument = KtPsiFactory(project).createArgument(prompt)
    val documentManager = PsiDocumentManager.getInstance(project)

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      documentManager.commitAllDocuments()
      promptPromptPsi.replace(newValueArgument)
    })

    return PromptExpression(
      oldExpression.functionSignature,
      element.valueArguments.firstOrNull()?.getBaseContentOffset() ?: 0,
      element.startOffset,
      element.endOffset,
      text,
      oldExpression.code
    )
  }
}