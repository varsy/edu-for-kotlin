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

/**
 * Adds a dependency mapping from the `from` element to the `to` element in the current `PsiElementToDependencies`
 * map, ensuring that the dependency is added only if it does not already exist.
 *
 * Additionally, if the `outerScopes` parameter is provided, the same dependency is added to each of the
 * outer scope dependencies recursively.
 *
 * If `to` is `null` `from` just added as a key for further use.
 *
 * @param from The source `PsiElement` representing the key in the dependencies map.
 * @param to The target `PsiElement` to be added as a dependency for the `from` element. Can be `null`.
 * @param outerScopes A collection of other `PsiElementToDependencies` maps representing outer scopes where
 * the dependency should also be added. Defaults to `null`.
 */
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
  when (this) {
    is KtReferenceExpression -> listOf(this)
    else -> PsiTreeUtil.findChildrenOfType(this, KtReferenceExpression::class.java)
      .filter { it !is KtOperationReferenceExpression }
  }

fun KtSingleValueToken?.isAssigment() = this != null && this.value in assignmentOperators

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
