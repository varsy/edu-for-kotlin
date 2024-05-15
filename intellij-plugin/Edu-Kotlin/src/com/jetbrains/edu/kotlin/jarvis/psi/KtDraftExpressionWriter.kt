package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DraftExpressionWriter
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jetbrains.kotlin.psi.KtPsiFactory

class KtDraftExpressionWriter : DraftExpressionWriter {

  override fun addDraftExpression(project: Project, element: PsiElement, generatedCode: String) {
    val draftTemplate = GeneratorUtils.getInternalTemplateText(DRAFT_BLOCK, mapOf(GENERATED_CODE_KEY to generatedCode))
    val psiFactory = KtPsiFactory(project)
    val draftExpression = psiFactory.createExpressionCodeFragment(draftTemplate, null)
    val newLine = psiFactory.createNewLine()
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      PsiDocumentManager.getInstance(project).commitAllDocuments()
      element.addAfter(draftExpression, element)
      element.addAfter(newLine, element)
    })
  }

  companion object {
    const val DRAFT_BLOCK = "DraftBlock.kt"
    const val GENERATED_CODE_KEY = "code"
  }
}
