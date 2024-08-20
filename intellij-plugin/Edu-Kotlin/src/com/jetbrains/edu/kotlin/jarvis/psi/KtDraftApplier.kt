package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.jarvis.DraftApplier

class KtDraftApplier : DraftApplier {

  override fun applyCodeDraftToMainCode(project: Project, element: PsiElement, psiFile: PsiFile?) {
    val draftBlock = ElementSearch.findDraftElement(element) { it.parent } ?: error("The draft block is not found")

    val lambdaArgument = draftBlock.lambdaArguments.firstOrNull() ?: return
    val lambdaBody = lambdaArgument.getLambdaExpression()?.bodyExpression ?: return

    val descriptionBlock = ElementSearch.findDescriptionElement(draftBlock) { it.prevSibling } ?: error("The description block is not found")

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      descriptionBlock.replace(lambdaBody)
      draftBlock.delete()
    }, psiFile)
  }
}
