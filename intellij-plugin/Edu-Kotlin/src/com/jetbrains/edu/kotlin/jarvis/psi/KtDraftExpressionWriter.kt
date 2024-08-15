package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.kotlin.jarvis.psi.ElementSearch.getDraftBlock
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReturnExpression

class KtDraftExpressionWriter : DraftExpressionWriter {

  override fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String, returnType: String): Int {
    val psiFactory = KtPsiFactory(project)

    val returnDraftTemplate = getReturnDraftTemplate(generatedCode, returnType)

    val newReturnDraftBlock = psiFactory.createExpression(returnDraftTemplate)
    val documentManager = PsiDocumentManager.getInstance(project)
    val newLine = psiFactory.createNewLine()

    var codeOffset = 0

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      documentManager.commitAllDocuments()
      val existingReturnDraftBlock = ElementSearch.findReturnDraftElement(element) { it.nextSibling }
      val returnDraftBlock = updateReturnDraftBlock(existingReturnDraftBlock, newReturnDraftBlock, newLine, element) as? KtReturnExpression
      codeOffset = returnDraftBlock
        ?.getDraftBlock()
        ?.getBodyExpression()
        ?.textOffset ?: 0
    })
    return codeOffset
  }

  private fun KtCallExpression.getBodyExpression(): KtExpression? =
    lambdaArguments
    .firstOrNull()
    ?.getLambdaExpression()
    ?.bodyExpression

  private fun getReturnDraftTemplate(generatedCode: String, returnType: String): String {
    return GeneratorUtils.getInternalTemplateText(DRAFT_BLOCK,
      mapOf(GENERATED_CODE_KEY to generatedCode, RETURN_TYPE_KEY to returnType))
  }

  private fun updateReturnDraftBlock(
    existingReturnDraftBlock: KtReturnExpression?,
    newReturnDraftBlock: KtExpression,
    newLine: PsiElement,
    element: PsiElement)
    = when (existingReturnDraftBlock) {
      null -> createElementParent(newReturnDraftBlock, newLine, element)
      else -> existingReturnDraftBlock.replace(newReturnDraftBlock)
    }


  private fun createElementParent(newReturnDraftBlock: KtExpression, newLine: PsiElement, element: PsiElement): PsiElement {
    element.parent.addAfter(newReturnDraftBlock, element)
    return element.parent.addAfter(newLine, element)
  }

  companion object {
    const val DRAFT_BLOCK = "DraftBlock.kt"
    const val GENERATED_CODE_KEY = "code"
    const val RETURN_TYPE_KEY = "returnType"

  }
}
