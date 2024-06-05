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
import org.jetbrains.kotlin.psi.KtPsiFactory

class KtDraftExpressionWriter : DraftExpressionWriter {

  override fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String) {
    val draftBlock = findBlock(element, { it.nextSibling }, DRAFT) as? KtCallExpression
    val draftTemplate = GeneratorUtils.getInternalTemplateText(draftBlock?.let { DRAFT_BODY } ?: DRAFT_BLOCK, mapOf(GENERATED_CODE_KEY to generatedCode))
    val psiFactory = KtPsiFactory(project)
    val draftExpression = draftBlock?.let { psiFactory.createBlock(draftTemplate) } ?: psiFactory.createExpressionCodeFragment(draftTemplate, null)
    val newLine = psiFactory.createNewLine()
    val oldDraftBody = draftBlock?.lambdaArguments?.firstOrNull()
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      PsiDocumentManager.getInstance(project).commitAllDocuments()
      if (oldDraftBody == null && draftBlock == null) {
        element.addAfter(draftExpression, element)
        element.addAfter(newLine, element)
      } else {
        oldDraftBody?.replace(draftExpression)
      }
    })
  }

  companion object {
    const val DRAFT_BLOCK = "DraftBlock.kt"
    const val DRAFT_BODY = "DraftBody.kt"
    const val GENERATED_CODE_KEY = "code"
  }
}
