package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

class KtDraftExpressionWriter : DraftExpressionWriter {

  override fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String): Int {
    val psiFactory = KtPsiFactory(project)

    val draftTemplate = getDraftTemplate(generatedCode)

    val newDraftBlock = psiFactory.createExpression(draftTemplate) as? KtCallExpression ?: error("Failed to create draft block")
    val documentManager = PsiDocumentManager.getInstance(project)
    val newLine = psiFactory.createNewLine()

    var codeOffset = 0

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      documentManager.commitAllDocuments()
      val existingDraftBlock = ElementSearch.findDraftElement(element) { it.nextSibling }
      val resultingDraftBlock =
        updateDraftBlock(existingDraftBlock, newDraftBlock, newLine, element)
      codeOffset = resultingDraftBlock.getCodeOffset()
    })
    return codeOffset
  }

  private fun KtCallExpression.getCodeOffset(): Int = getBodyExpression()?.textOffset ?: 0

  private fun KtCallExpression.getBodyExpression(): KtExpression? =
    lambdaArguments
    .firstOrNull()
    ?.getLambdaExpression()
    ?.bodyExpression

  private fun getDraftTemplate(generatedCode: String): String {
    return GeneratorUtils.getInternalTemplateText(DRAFT_BLOCK,
      mapOf(GENERATED_CODE_KEY to generatedCode))
  }

  private fun updateDraftBlock(
    existingDraftBlock: KtCallExpression?,
    newDraftBlock: KtExpression,
    newLine: PsiElement,
    element: PsiElement)
    = when (existingDraftBlock) {
      null -> createElementParent(newDraftBlock, newLine, element)
      else -> existingDraftBlock.replace(newDraftBlock)
    } as? KtCallExpression ?: error("Failed to create draft block")


  private fun createElementParent(newDraftBlock: KtExpression, newLine: PsiElement, element: PsiElement): PsiElement {
    val createdElement = element.parent.addAfter(newDraftBlock, element)
    element.parent.addAfter(newLine, element)
    return createdElement
  }

  companion object {
    const val DRAFT_BLOCK = "DraftBlock.kt"
    const val GENERATED_CODE_KEY = "code"
  }
}
