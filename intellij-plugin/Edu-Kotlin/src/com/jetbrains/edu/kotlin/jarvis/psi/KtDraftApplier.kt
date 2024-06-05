package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.childrenOfType
import com.jetbrains.edu.jarvis.DraftApplier
import com.jetbrains.edu.kotlin.jarvis.utils.DESCRIPTION
import com.jetbrains.edu.kotlin.jarvis.utils.DRAFT
import com.jetbrains.edu.kotlin.jarvis.utils.findBlock
import org.jetbrains.kotlin.psi.KtCallExpression

class KtDraftApplier : DraftApplier {

  override fun applyCodeDraftToMainCode(project: Project, element: PsiElement, psiFile: PsiFile?) {
    val draftBlock = findBlock(element, { it.parent }, DRAFT) as? KtCallExpression ?: error("The draft block is not found")
    val lambdaArgument = draftBlock.lambdaArguments.firstOrNull() ?: return
    val lambdaBody = lambdaArgument.getLambdaExpression()?.bodyExpression ?: return
    // TODO: parse these comments
    val commentsToRemove = lambdaBody.childrenOfType<PsiComment>().take(NUM_COMMENTS_TO_REMOVE)
    val descriptionBlock = findBlock(draftBlock, { it.prevSibling }, DESCRIPTION) ?: error("The description block is not found")
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      commentsToRemove.forEach { it.delete() }
      descriptionBlock.replace(lambdaBody)
      draftBlock.delete()
    }, psiFile)
  }

  companion object {
    private const val NUM_COMMENTS_TO_REMOVE = 3
  }
}
