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

      is KtBinaryExpression -> psiElement.resolveReferencesLines(document)

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

      is KtWhileExpressionBase -> buildList {
        psiElement.body
          ?.let { document.getLineWithBlockAdjustment(it) }
          ?.let { add(it) }
        psiElement.condition
          ?.resolveReferencesLines(document)
          ?.let { addAll(it) }
      }

      else -> emptyList()
    }

  private fun PsiElement.resolveReferencesLines(document: Document) =
    getNameReferenceExpressions().mapNotNull { referenceExpression ->
      referenceExpression?.reference?.resolve()?.let { document.getAllReferencesLines(it) }
    }.flatten().distinct()

  private fun PsiElement.getNameReferenceExpressions() =
    PsiTreeUtil.collectElementsOfType(this, KtNameReferenceExpression::class.java)

  private fun KtIfExpression.getIfEntries(): List<KtExpression> =
    when (val elseExpression = `else`) {
      is KtIfExpression -> listOfNotNull(then) + elseExpression.getIfEntries()
      else -> listOfNotNull(then, `else`)
    }

  private fun Document.getLineWithBlockAdjustment(expression: KtExpression) =
    if (expression is KtBlockExpression) getLineNumber(expression) + 1
    else getLineNumber(expression)

  override fun getCalleeExpressions(psiFile: PsiFile): List<PsiElement> =
    PsiTreeUtil.collectElementsOfType(psiFile, KtCallExpression::class.java).mapNotNull { it.calleeExpression }

  override fun getParentFunctionName(element: PsiElement): String? =
    PsiTreeUtil.getParentOfType(element, KtNamedFunction::class.java)?.name

}
