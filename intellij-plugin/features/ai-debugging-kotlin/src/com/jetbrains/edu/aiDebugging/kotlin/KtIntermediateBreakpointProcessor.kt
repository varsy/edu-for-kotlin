package com.jetbrains.edu.aiDebugging.kotlin

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiDebugging.core.breakpoint.IntermediateBreakpointProcessor
import org.jetbrains.kotlin.psi.*
import com.intellij.psi.util.PsiTreeUtil

class KtIntermediateBreakpointProcessor : IntermediateBreakpointProcessor() {
  override fun findBreakpointLines(psiElement: PsiElement, document: Document, psiFile: PsiFile): List<Int> =
    when (psiElement) {

      is KtProperty -> document.getAllReferencesLines(psiElement)

      is KtBinaryExpression -> (psiElement.left as? KtNameReferenceExpression)
                                 ?.reference?.resolve()
                                 ?.let { document.getAllReferencesLines(it) } ?: emptyList()

      is KtForExpression -> psiElement.loopParameter?.let { document.getAllReferencesLines(it) } ?: emptyList()

      is KtFunction -> {
        val functionCalls = PsiTreeUtil.collectElementsOfType(psiFile, KtCallExpression::class.java).toList()
          .filter { it.calleeExpression?.text == psiElement.name }
          .mapNotNull { document.getLineNumber(it) }
        psiElement.bodyExpression
          ?.let { document.getLineWithBlockAdjustment(it) }
          ?.let { listOf(it) + functionCalls } ?: functionCalls
      }

      is KtIfExpression -> psiElement.getIfEntries().map { document.getLineWithBlockAdjustment(it) }.distinct()

      is KtWhenExpression -> psiElement.entries.mapNotNull { entry ->
        entry.expression?.let { document.getLineWithBlockAdjustment(it) }
      }.distinct()

      is KtWhileExpressionBase -> psiElement.body
        ?.let { document.getLineWithBlockAdjustment(it) }
        ?.let { listOf(it) } ?: emptyList()

      else -> emptyList()
    }

  private fun KtIfExpression.getIfEntries(): List<KtExpression> =
    when (val elseExpression = `else`) {
      is KtIfExpression -> listOfNotNull(then) + elseExpression.getIfEntries()
      else -> listOfNotNull(then, `else`)
    }

  private fun Document.getLineWithBlockAdjustment(expression: KtExpression) =
    if (expression is KtBlockExpression) getLineNumber(expression) + 1
    else getLineNumber(expression)

}
