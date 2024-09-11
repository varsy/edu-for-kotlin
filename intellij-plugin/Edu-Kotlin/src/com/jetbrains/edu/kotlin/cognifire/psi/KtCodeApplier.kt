package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.cognifire.CodeApplier

class KtCodeApplier : CodeApplier {

  override fun applyDraftCodeToMainCode(project: Project, element: PsiElement, psiFile: PsiFile?) {
    val codeBlock = ElementSearch.findCodeElement(element) { it.parent } ?: error("The code block is not found")

    val lambdaArgument = codeBlock.lambdaArguments.firstOrNull() ?: return
    val lambdaBody = lambdaArgument.getLambdaExpression()?.bodyExpression ?: return

    val promptBlock = ElementSearch.findPromptElement(codeBlock) { it.prevSibling } ?: error("The prompt block is not found")

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      promptBlock.replace(lambdaBody)
      codeBlock.delete()
    }, psiFile)
  }
}
