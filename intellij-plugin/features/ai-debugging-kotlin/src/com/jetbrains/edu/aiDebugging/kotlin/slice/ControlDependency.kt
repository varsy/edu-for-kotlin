package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement
import com.jetbrains.edu.aiDebugging.core.slicing.PsiElementToDependencies
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle

class ControlDependency(psiElement: PsiElement) {
  val dependenciesForward = mutableMapOf<PsiElement, HashSet<PsiElement>>()
  val dependenciesBackward = mutableMapOf<PsiElement, HashSet<PsiElement>>()

  init {
    processControlDependency(psiElement)
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
            addDependency(psiElement, it)
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
      addDependency(psiElement, it)
      processControlDependency(it)
    }
  }

  fun Sequence<PsiElement>.forEachReachable(action: (PsiElement) -> Unit) = iterator().forEachReachable(action)

  fun Array<PsiElement>.forEachReachable(action: (PsiElement) -> Unit) = iterator().forEachReachable(action)

  fun Iterator<PsiElement>.forEachReachable(action: (PsiElement) -> Unit) {
    for (element in this) {
      if (element is KtReturnExpression || element is KtContinueExpression || element is KtBreakExpression) {
        break
      }
      action(element)
    }
  }

  private fun addDependency(psiElement1: PsiElement, psiElement2: PsiElement) {
    dependenciesForward.addIfAbsent(psiElement1, psiElement2)
    dependenciesBackward.addIfAbsent(psiElement2, psiElement1)
  }

  private fun PsiElementToDependencies.addIfAbsent(psiElement1: PsiElement, psiElement2: PsiElement) {
    if (!this.contains(psiElement1)) {
      this[psiElement1] = hashSetOf(psiElement2)
    }
    else {
      this[psiElement1]?.add(psiElement2)
    }
  }

}