package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.CodeExpressionWriter
import com.jetbrains.edu.jarvis.models.CodeExpression
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class KtCodeExpressionWriter : CodeExpressionWriter {

  override fun addCodeExpression(project: Project, element: PsiElement, generatedCode: String): CodeExpression {
    val psiFactory = KtPsiFactory(project)

    val codeTemplate = getCodeTemplate(generatedCode)

    val newCodeBlock = psiFactory.createExpression(codeTemplate) as? KtCallExpression ?: error("Failed to create code block")
    val documentManager = PsiDocumentManager.getInstance(project)
    val newLine = psiFactory.createNewLine()

    var codeOffset = element.startOffset
    val startOffset = element.startOffset
    var endOffset = element.endOffset

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      documentManager.commitAllDocuments()
      val existingCodeBlock = ElementSearch.findCodeElement(element) { it.nextSibling }
      val resultingCodeBlock =
        updateCodeBlock(existingCodeBlock, newCodeBlock, newLine, element)

      codeOffset = resultingCodeBlock.getCodeOffset()
      endOffset = resultingCodeBlock.endOffset
    })
    return CodeExpression(
      generatedCode,
      codeOffset,
      startOffset,
      endOffset
    )
  }

  private fun KtCallExpression.getCodeOffset(): Int = getBodyExpression()?.textOffset ?: 0

  private fun KtCallExpression.getBodyExpression(): KtExpression? =
    lambdaArguments
    .firstOrNull()
    ?.getLambdaExpression()
    ?.bodyExpression

  private fun getCodeTemplate(generatedCode: String): String {
    return GeneratorUtils.getInternalTemplateText(CODE_BLOCK,
      mapOf(GENERATED_CODE_KEY to generatedCode))
  }

  private fun updateCodeBlock(
    existingCodeBlock: KtCallExpression?,
    newCodeBlock: KtExpression,
    newLine: PsiElement,
    element: PsiElement)
    = when (existingCodeBlock) {
      null -> createElementParent(newCodeBlock, newLine, element)
      else -> existingCodeBlock.replace(newCodeBlock)
    } as? KtCallExpression ?: error("Failed to create code block")


  private fun createElementParent(newCodeBlock: KtExpression, newLine: PsiElement, element: PsiElement): PsiElement {
    val createdElement = element.parent.addAfter(newCodeBlock, element)
    element.parent.addAfter(newLine, element)
    return createdElement
  }

  companion object {
    const val CODE_BLOCK = "CodeBlock.kt"
    const val GENERATED_CODE_KEY = "code"
  }
}
