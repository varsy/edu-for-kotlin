package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle

/**
 * Represents a control dependency for a Kotlin function, enabling the construction of dependency graphs.
 *
 * This class computes forward and backward control-dependency relationships between various code elements
 * within the given Kotlin function (`KtFunction`).
 *
 * @constructor Initializes the dependency analysis for the provided function and computes
 * control dependencies for all reachable code elements.
 *
 * @param ktFunction The Kotlin function for which control dependencies are analyzed.
 *
 * @property dependenciesForward A mapping from each code element to the set of other elements
 * it forwards control flow to.
 *
 * @property dependenciesBackward A mapping from each code element to the set of other elements
 * it receives control flow from.
 */
class FunctionControlDependency(ktFunction: KtFunction) : FunctionDependency() {

  init {
    processControlDependency(ktFunction)
  }

  private fun processControlDependency(psiElement: PsiElement) {
    when (psiElement) {

      is KtFunction -> {
        psiElement.children.forEach { processControlDependency(it) }
      }

      is KtForExpression -> {
        psiElement.body?.addAndProcesNext(psiElement)
      }

      is KtIfExpression -> {
        psiElement.then?.addAndProcesNext(psiElement)
        psiElement.`else`?.addAndProcesNext(psiElement)
      }

      is KtWhenExpression -> {
        psiElement.entries.forEach { entry ->
          entry.expression?.blockExpressionsOrSingle()?.forEachReachable {
            psiElement.addDependency(it)
            processControlDependency(it)
          }
        }
      }

      is KtWhileExpressionBase -> {
        psiElement.body?.addAndProcesNext(psiElement)
      }

      else -> psiElement.children.forEachReachable { processControlDependency(it) }
    }
  }

  private fun PsiElement.addAndProcesNext(psiElement: PsiElement) {
    children.forEachReachable {
      psiElement.addDependency(it)
      processControlDependency(it)
    }
  }

  override fun PsiElement.addDependency(other: PsiElement) {
    dependenciesForward.addIfAbsent(this, other)
    dependenciesBackward.addIfAbsent(other, this)
  }
}
