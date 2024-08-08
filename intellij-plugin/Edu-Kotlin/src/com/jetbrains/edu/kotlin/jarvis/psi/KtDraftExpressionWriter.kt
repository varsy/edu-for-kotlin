package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.kotlin.jarvis.utils.DRAFT
import com.jetbrains.edu.kotlin.jarvis.utils.findBlock
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

class KtDraftExpressionWriter : DraftExpressionWriter {

  override fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String): Int {
    val psiFactory = KtPsiFactory(project)
    val draftTemplate = GeneratorUtils.getInternalTemplateText(DRAFT_BLOCK, mapOf(GENERATED_CODE_KEY to generatedCode))
    val newDraftBlock = psiFactory.createExpression(draftTemplate)
    val documentManager = PsiDocumentManager.getInstance(project)
    val newLine = psiFactory.createNewLine()

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      documentManager.commitAllDocuments()
      val existingDraftBlock = findBlock(element, PsiElement::getNextSibling, DRAFT) as? KtCallExpression
      when (val existingDraftBody = getBodyExpression(existingDraftBlock)) {
        null -> {
          element.parent.addAfter(newDraftBlock, element)
          element.parent.addAfter(newLine, element)
        }

        else -> {
          val newDraftBody = getBodyExpression(newDraftBlock as? KtCallExpression)
          newDraftBody?.let {
            existingDraftBody.replace(it)
          }
        }
      }
    })
    val draftBlock = findBlock(element, { it.nextSibling }, DRAFT) as? KtCallExpression
    return getBodyExpression(draftBlock)?.textOffset ?: error("Can't find body draft expression")
  }

  private fun getBodyExpression(callExpression: KtCallExpression?): KtExpression? =
    callExpression
      ?.lambdaArguments
      ?.firstOrNull()
      ?.getLambdaExpression()
      ?.bodyExpression

  companion object {
    const val DRAFT_BLOCK = "DraftBlock.kt"
    const val GENERATED_CODE_KEY = "code"
  }
}
