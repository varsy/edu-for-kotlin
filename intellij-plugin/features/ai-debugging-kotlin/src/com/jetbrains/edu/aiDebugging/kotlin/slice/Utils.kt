package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression

private val reflectedAssignmentOperators = listOf("+=", "*=", "-=", "/=")
private val assignmentOperators = listOf("=") + reflectedAssignmentOperators

fun PsiElementToDependencies.addIfAbsent(
  from: PsiElement,
  to: PsiElement? = null,
  outerScopes: Collection<PsiElementToDependencies>? = null
) {
  getOrPut(from) { hashSetOf() }.also { collection ->
    to?.let {
      collection.add(it)
    }
  }
  outerScopes?.forEach {
    it.addIfAbsent(from, to)
  }
}

fun PsiElement.getVariableReferences() =
  if (this is KtReferenceExpression) {
    listOf(this)
  }
  else {
    PsiTreeUtil.findChildrenOfType(this, KtReferenceExpression::class.java)
      .filter { it !is KtOperationReferenceExpression }
  }

fun KtSingleValueToken?.isChange() = this != null && this.value in assignmentOperators

fun PsiElementToDependencies.copy() = mapValues { it.value.toHashSet() }.toMutableMap()

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
