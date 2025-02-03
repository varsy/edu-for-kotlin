package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.completion.reference
import org.jetbrains.kotlin.psi.*

/**
 * Represents a data dependency analyzer for a Kotlin function, allowing the construction of data dependency graphs.
 *
 * This class computes forward and backward data-dependency relationships between various code elements
 * within the given Kotlin function (`KtFunction`).
 *
 * @constructor Initializes the dependency analysis for the provided function and computes
 * data dependencies for all referenced code elements.
 *
 * @param psiElement The root PSI element (commonly a function) for which data dependencies are analyzed.
 *
 * @property dependenciesForward A mapping from each code element to the set of other code elements
 * to which its data dependencies are forwarded.
 *
 * @property dependenciesBackward A mapping from each code element to the set of other code elements
 * from which it receives data dependencies.
 */
class FunctionDataDependency(psiElement: PsiElement) : FunctionDependency() {

  init {
    processReferences(psiElement, mutableMapOf())
  }


  /**
   * Processes the references within the provided PsiElement, managing dependencies and scope definitions.
   *
   * @param psiElement The PSI element to be analyzed and processed for references.
   * @param definedVariables A mapping of PSI elements to their defined references.
   * @param outerScopeDefinitions A list of dependencies from outer scopes to be considered during processing. Defaults to an empty list.
   */
  private fun processReferences(
    psiElement: PsiElement,
    definedVariables: PsiElementToDependencies,
    outerScopeDefinitions: List<PsiElementToDependencies> = emptyList()
  ) {
    when (psiElement) {

      is KtFunction -> {
        psiElement.valueParameterList?.parameters?.forEach {
          definedVariables.addIfAbsent(it, outerScopes = outerScopeDefinitions)
        }
        psiElement.bodyBlockExpression?.statements?.forEach {
          processReferences(it, definedVariables, outerScopeDefinitions)
        }
      }

      is KtProperty -> {
        definedVariables.addIfAbsent(psiElement, outerScopes = outerScopeDefinitions)
        psiElement.initializer?.let {
          it.getVariableReferences().forEach { reference ->
            psiElement.addReferences(reference, definedVariables)
          }
        }
      }

      is KtBinaryExpression -> {
        if (psiElement.operationReference.operationSignTokenType.isChange()) {
          psiElement.left?.reference()?.resolve()?.let { referenceTo ->
            definedVariables[referenceTo]?.forEach {
              if (psiElement != it) {
                psiElement.addDependency(it)
              }
            }
            psiElement.addDependency(referenceTo)
            definedVariables.addIfAbsent(referenceTo, psiElement, outerScopeDefinitions)
          }
          psiElement.right?.getVariableReferences()?.forEach {
            psiElement.addReferences(it, definedVariables)
          }
        }
      }

      is KtPostfixExpression -> {
        psiElement.baseExpression?.reference()?.resolve()?.let {
          definedVariables.addIfAbsent(it, psiElement, outerScopeDefinitions)
          psiElement.addDependency(it)
        }
      }

      is KtForExpression -> {
        psiElement.loopParameter?.let {
          definedVariables.addIfAbsent(it, outerScopes = outerScopeDefinitions)
        }
        psiElement.loopRange?.getVariableReferences()?.forEach {
          psiElement.addReferences(it, definedVariables)
        }
        psiElement.body?.children?.forEach {
          definedVariables.searchElements(it, outerScopeDefinitions)
        }
        psiElement.body?.children?.forEach {
          processReferences(it, definedVariables, outerScopeDefinitions)
        }
      }

      is KtWhileExpression -> {
        psiElement.body?.children?.forEach {
          definedVariables.searchElements(it, outerScopeDefinitions)
        }

        psiElement.condition?.getVariableReferences()?.forEach {
          psiElement.addReferences(it, definedVariables)
        }
        psiElement.body?.children?.forEach {
          processReferences(it, definedVariables, outerScopeDefinitions)
        }
      }

      is KtCallExpression -> {
        psiElement.valueArgumentList?.arguments?.forEach {
          it.getArgumentExpression()?.getVariableReferences()?.forEach { reference ->
            psiElement.addReferences(reference, definedVariables)
          }
        }
      }

      is KtWhenExpression -> {
        psiElement.subjectExpression
          ?.takeIf { it is KtReferenceExpression }
          ?.let { psiElement.addReferences(it as KtReferenceExpression, definedVariables) }
        val stateSnapshot = definedVariables.copy()
        psiElement.entries.forEach { entry ->
          val expression = entry.expression
          when (expression) {
            is KtBlockExpression -> {
              val blockState = stateSnapshot.copy()
              expression.statements.forEach { statement ->
                processReferences(statement, blockState, outerScopeDefinitions + definedVariables)
              }
            }

            else -> {
              expression?.let { processReferences(it, stateSnapshot.copy(), outerScopeDefinitions + definedVariables) }
            }
          }
        }
      }

      is KtIfExpression -> { // TODO add elif expression
        psiElement.condition?.getVariableReferences()?.forEach {
          psiElement.addReferences(it, definedVariables)
        }
        val stateSnapshot = definedVariables.copy()
        psiElement.then?.let {
          val brachState = stateSnapshot.copy()
          it.children.forEach { element ->
            processReferences(element, brachState, outerScopeDefinitions + definedVariables)
          }
        }
        psiElement.`else`?.let {
          val brachState = stateSnapshot.copy()
          it.children.forEach { element ->
            processReferences(element, brachState, outerScopeDefinitions + definedVariables)
          }
        }
      }
    }
  }

  private fun PsiElement.addReferences(other: KtReferenceExpression, definedVariables: PsiElementToDependencies) {
    other.reference()?.resolve()?.let { referenceTo ->
      addDependency(referenceTo)
      definedVariables[referenceTo]?.forEach {
        addDependency(it)
      }
    }
  }

  private fun PsiElementToDependencies.searchElements(
    psiElement: PsiElement,
    outerScopeDefinitions: List<PsiElementToDependencies>? = null
  ) {
    when (psiElement) {
      is KtBinaryExpression -> {
        if (psiElement.operationReference.operationSignTokenType.isChange()) {
          psiElement.left?.reference()?.resolve()?.let {
            addIfAbsent(it, psiElement, outerScopeDefinitions)
          }
        }
      }

      is KtPostfixExpression -> {
        psiElement.baseExpression?.reference()?.resolve()?.let {
          addIfAbsent(it, psiElement, outerScopeDefinitions)
        }
      }
    }
  }
}
