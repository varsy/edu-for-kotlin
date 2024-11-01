package com.jetbrains.edu.aiHints.kotlin

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.aiHints.core.FunctionDiffReducer
import org.jetbrains.kotlin.psi.*

class KtFunctionDiffReducer : FunctionDiffReducer {

  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement, project: Project): PsiElement {
    if (function == null) {
      reducingNewElement(modifiedFunction, project, true)
      return modifiedFunction
    }
    reducingReplacementElement(function, modifiedFunction, project, true)
    return function
  }

  private fun compareAndAmendChildren(
    first: Array<PsiElement>,
    second: Array<PsiElement>,
    project: Project,
    insertionAfterElement: PsiElement?,
    needLineBreak: Boolean = true
  ): Boolean {
    first.zip(second).forEach {
      if (!Regex(TODO).containsMatchIn(it.first.text) && it.first.node.elementType != it.second.node.elementType) {
        addElement(it.first, it.second, project, addAfter = false, needLineBreak = needLineBreak)
        return true
      }
      if (!equalText(it.first, it.second, Char::isWhitespace)) {
        swapElements(it.first, it.second, project)
        return true
      }
    }

    if (first.size < second.size) {
      if (first.isEmpty()) {
        insertionAfterElement?.let {
          addElement(it, second[first.size], project, needLineBreak = needLineBreak)
          return true
        }
      }
      else {
        addElement(first.last(), second[first.size], project, needLineBreak = needLineBreak)
        return true
      }
    }
    return false
  }

  private fun performWriteAction(project: Project, action: () -> Unit) {
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      PsiDocumentManager.getInstance(project).commitAllDocuments()
      action()
    })
  }

  private fun addElement(first: PsiElement, second: PsiElement, project: Project, addAfter: Boolean = true, needLineBreak: Boolean = true) {
    reducingNewElement(second, project)
    val parent = first.parent
    val newLine = KtPsiFactory(project).createNewLine()
    performWriteAction(project) {
      if (addAfter) {
        parent.addAfter(second.copy(), first)
        if (needLineBreak) {
          parent.addAfter(newLine, first)
        }
      }
      else {
        parent.addBefore(second.copy(), first)
        if (needLineBreak) {
          parent.addBefore(newLine, first)
        }
      }
    }
  }

  private fun swapElements(first: PsiElement, second: PsiElement, project: Project) {
    if (!reducingReplacementElement(first, second, project)) {
      performWriteAction(project) { first.replace(second) }
    }
  }

  private fun removeElement(element: PsiElement, project: Project) {
    performWriteAction(project) { element.delete() }
  }

  private fun reducingNewElement(element: PsiElement, project: Project, downsize: Boolean = false) {
    val size = element.text?.lines()?.size ?: error("Cannot get the body size of $element")
    if (size <= MAX_BODY_LINES_IN_SHORT_FUNCTION && !downsize) return

    when (element) {
      // If a new function has been added - leave only the function declaration - change the function body to TODO_EXPRESSION
      is KtNamedFunction -> {
        val todoExpression = if (element.hasBlockBody()) {
          KtPsiFactory(project).createExpression(TODO_BLOCK_EXPRESSION.trimIndent())
        }
        else {
          KtPsiFactory(project).createExpression(TODO_EXPRESSION)
        }
        element.bodyExpression?.let { swapElements(it, todoExpression, project) }
      }

      // If a new `while` expression has been added - delete its body - leave only the condition statement
      is KtWhileExpressionBase -> {
        element.body?.let { swapElements(it, KtPsiFactory(project).createEmptyBody(), project) }
      }

      // If a new `for` expression has been added - delete its body - leave only the loopRange and loopParameter
      is KtForExpression -> element.body?.let {
        swapElements(it, KtPsiFactory(project).createEmptyBody(), project)
      }

      // If a new `if` expression has been added - delete its `then` block and `else` block - leave only the condition statement
      is KtIfExpression -> {
        element.then?.let { swapElements(it, KtPsiFactory(project).createEmptyBody(), project) }
        element.`else`?.let { removeElement(it, project) }
        element.elseKeyword?.let { removeElement(it, project) }
      }

      // If a new `when` expression has been added - delete its entries - leave only the subjectExpression
      is KtWhenExpression -> element.entries.forEach { removeElement(it, project) }

      // If a new `return` expression has been added - move on to the reduction of the return expression
      is KtReturnExpression -> element.returnedExpression?.let { reducingNewElement(it, project) }
    }
  }

  private fun reducingReplacementElement(first: PsiElement, second: PsiElement, project: Project, downsize: Boolean = false): Boolean {
    val size = second.text?.lines()?.size ?: error("Cannot get the body size of $second")
    if (size > MAX_BODY_LINES_IN_SHORT_FUNCTION || downsize) {
      // Find which element of the expression in `second` has changed from `first` first from top to bottom and add this change to `first`
      return when {
        // Change the function parameters or body
        first is KtNamedFunction && second is KtNamedFunction ->
          swapSmallElements(first.valueParameterList, second.valueParameterList, project) ||
          swapSmallElements(first.typeReference, second.typeReference, project) ||
          resolveMultilineMismatch(first.bodyExpression, second.bodyExpression, project)
        // Change condition or body
        first is KtWhileExpressionBase && second is KtWhileExpressionBase ->
          swapSmallElements(first.condition, second.condition, project) ||
          resolveMultilineMismatch(first.body, second.body, project)
        // Change loopRange or loopParameter or body
        first is KtForExpression && second is KtForExpression ->
          swapSmallElements(first.loopRange, second.loopRange, project) ||
          swapSmallElements(first.loopParameter, second.loopParameter, project) ||
          resolveMultilineMismatch(first.body, second.body, project)
        // Change condition or then or else
        first is KtIfExpression && second is KtIfExpression ->
          swapSmallElements(first.condition, second.condition, project) ||
          resolveMultilineMismatch(first.then, second.then, project) ||
          swapSmallElements(first.`else`, second.`else`, project) ||
          addElseBlock(first.then, second.`else`, second.elseKeyword, project)
        // Change subjectExpression or entries
        first is KtWhenExpression && second is KtWhenExpression ->
          swapSmallElements(first.subjectExpression, second.subjectExpression, project) ||
          compareAndAmendChildren(
            first.entries.toTypedArray(),
            second.entries.toTypedArray(),
            project,
            first.openBrace,
            needLineBreak = false
          )
        // Move on to comparing the returned expressions
        first is KtReturnExpression && second is KtReturnExpression ->
          swapSmallElements(first.returnedExpression, second.returnedExpression, project)

        else -> false
      }
    }
    return false
  }

  private fun applyActionIfElementsDiffer(
    first: PsiElement?,
    second: PsiElement?,
    project: Project,
    action: (PsiElement?, PsiElement?, Project) -> Unit
  ): Boolean {
    if (first != null && second != null && !equalText(first, second, Char::isWhitespace)) {
      action(first, second, project)
      return true
    }
    return false
  }

  private fun swapSmallElements(first: PsiElement?, second: PsiElement?, project: Project) =
    applyActionIfElementsDiffer(first, second, project) { f, s, p ->
      swapElements(f!!, s!!, p)
    }

  private fun resolveMultilineMismatch(first: PsiElement?, second: PsiElement?, project: Project) =
    applyActionIfElementsDiffer(first, second, project) { f, s, p ->
      compareAndAmendChildren(f!!.children, s!!.children, p, f.firstChild)
    }

  private fun addElseBlock(then: KtExpression?, elseBlock: KtExpression?, elseKeyword: PsiElement?, project: Project): Boolean {
    if (then != null && elseBlock != null) {
      addElement(then, elseBlock, project, needLineBreak = false)
      elseKeyword?.let { addElement(then, it.copy(), project, needLineBreak = false) }
      return true
    }
    return false
  }

  private fun equalText(first: PsiElement, second: PsiElement, symbolsToIgnore: (Char) -> Boolean) =
    first.text.filterNot(symbolsToIgnore) == second.text.filterNot(symbolsToIgnore)

  companion object {
    private const val MAX_BODY_LINES_IN_SHORT_FUNCTION = 3
    private const val TODO = "TODO"
    private const val TODO_EXPRESSION = "TODO(\"Not yet implemented\")"
    private const val TODO_BLOCK_EXPRESSION =
      """
            {
                TODO("Not yet implemented")
            }
      """
  }
}
