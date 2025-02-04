package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.completion.reference
import org.jetbrains.kotlin.idea.intentions.branches
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.blockExpressionsOrSingle

/**
 * Represents a data dependency analyzer for a Kotlin function, allowing the construction of data dependency graphs.
 *
 * This class computes forward and backward data-dependency relationships between various code elements
 * within the given Kotlin function (`KtFunction`).
 *
 * @constructor Initializes the dependency analysis for the provided function and computes
 * data dependencies for all referenced code elements.
 *
 * @param ktFunction kotlin function for which data dependencies are analyzed.
 *
 * @property dependenciesForward A mapping from each code element to the set of other code elements
 * to which its data dependencies are forwarded.
 *
 * @property dependenciesBackward A mapping from each code element to the set of other code elements
 * from which it receives data dependencies.
 */
class FunctionDataDependency(ktFunction: KtFunction) : FunctionDependency() {

  init {
    processReferences(ktFunction, mutableMapOf())
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
    with(ScopeContext(definedVariables, outerScopeDefinitions)) {
      when (psiElement) {
        is KtFunction -> psiElement.process()

        is KtProperty -> psiElement.process()

        is KtBinaryExpression -> psiElement.process()

        is KtPostfixExpression -> psiElement.process()

        is KtForExpression -> psiElement.process()

        is KtWhileExpression -> psiElement.process()

        is KtCallExpression -> psiElement.process()

        is KtWhenExpression -> psiElement.process()

        // TODO add elif expression
        is KtIfExpression -> psiElement.process()
      }
    }
  }

  /**
   * A context to manage the scope and defined variables during current state of processing.
   */
  private inner class ScopeContext(
    val definedVariables: PsiElementToDependencies,
    val outerScopeDefinitions: List<PsiElementToDependencies>
  ) {
    fun KtFunction.process() {
      valueParameters.forEach {
        definedVariables.addIfAbsent(it, outerScopes = outerScopeDefinitions)
      }

      bodyExpression?.blockExpressionsOrSingle()?.forEach {
        processReferences(it, definedVariables, outerScopeDefinitions)
      }
    }

    /**
     * The method registers the property into the [definedVariables]
     * and adding dependencies of references in initializer
     */
    fun KtProperty.process() {
      definedVariables.addIfAbsent(this, outerScopes = outerScopeDefinitions)
      initializer?.let {
        it.getVariableReferences().forEach { reference ->
          addReferences(reference, definedVariables)
        }
      }
    }

    /**
     * This function looks for the corresponding property on the left side
     * of the expression and also looks for all used references.
     * The right side looks for all used variables and defined references.
     * Dependencies are created on this basis.
     *
     * Do not use [addReferences] because exactly BinaryExpression should be added for dependencies
     * And it should not contain cyclic dependencies.
     */
    fun KtBinaryExpression.process() {
      if (operationReference.operationSignTokenType.isAssigment()) {
        left?.reference()?.resolve()?.let { referenceTo ->
          definedVariables[referenceTo]?.forEach {
            if (this != it) {
              addDependency(it)
            }
          }
          addDependency(referenceTo)
          definedVariables.addIfAbsent(referenceTo, this, outerScopeDefinitions)
        }
        right?.getVariableReferences()?.forEach {
          addReferences(it, definedVariables)
        }
      }
    }

    /**
     * This method resolves the reference from the unary expression base element and updates dependency mappings.
     */
    fun KtPostfixExpression.process() {
      baseExpression?.reference()?.resolve()?.let {
        definedVariables.addIfAbsent(it, this, outerScopeDefinitions)
        addDependency(it)
      }
    }

    /**
     * This method performs the following tasks:
     * - Registers the loop parameter in the `definedVariables` map.
     * - Resolves variable references from the loop range and updates the dependency mappings.
     * - Iteratively processes child elements within the body for variable definitions and further scope references.
     */
    fun KtForExpression.process() {
      loopParameter?.let {
        definedVariables.addIfAbsent(it, outerScopes = outerScopeDefinitions)
      }
      loopRange?.getVariableReferences()?.forEach {
        addReferences(it, definedVariables)
      }
      body?.children?.forEach {
        definedVariables.searchElements(it)
      }
      body?.children?.forEach {
        processReferences(it, definedVariables, outerScopeDefinitions)
      }
    }

    /**
     * This method performs the following tasks:
     * - Iterates over the children of the `body`, utilizing `searchElements` to handle variable definitions
     *   in the current and outer scope contexts.
     * - Processes references retrieved from the `condition`, updating the dependency mappings via `addReferences`.
     * - Further processes each element of the `body` via `processReferences` to manage scoped dependencies
     *   and nested structures.
     */
    fun KtWhileExpression.process() {
      body?.children?.forEach {
        definedVariables.searchElements(it)
      }

      condition?.getVariableReferences()?.forEach {
        addReferences(it, definedVariables)
      }
      body?.children?.forEach {
        processReferences(it, definedVariables, outerScopeDefinitions)
      }
    }


    /**
     * This method performs the following tasks:
     * - Iterates over the arguments in the `valueArgumentList` of the call expression.
     * - For each argument, extracts its expression and retrieves any variable references.
     * - Each identified reference is processed by invoking `addReferences`, adding their dependencies.
     */
    fun KtCallExpression.process() {
      valueArgumentList?.arguments?.forEach {
        it.getArgumentExpression()?.getVariableReferences()?.forEach { reference ->
          addReferences(reference, definedVariables)
        }
      }
    }

    /**
     * This method performs the following operations:
     * - Checks if the `subjectExpression` is a `KtReferenceExpression` and
     *   calls `addReferences` to record its dependencies.
     * - If an entry's `expression` is a `KtBlockExpression`, processes each statement within the block.
     * - For other expression types, calls `processReferences` to resolve and update references,
     *     working within an updated snapshot of `definedVariables` and `outerScopeDefinitions`.
     */
    fun KtWhenExpression.process() {
      subjectExpression
        ?.takeIf { it is KtReferenceExpression }
        ?.let { addReferences(it as KtReferenceExpression, definedVariables) }
      val stateSnapshot = definedVariables.copy() // need to be copied due to execution branches
      entries.forEach { entry ->
        val blockState = stateSnapshot.copy()
        entry.expression?.blockExpressionsOrSingle()?.forEach { statement ->
          processReferences(statement, blockState, outerScopeDefinitions + definedVariables)
        }
      }
    }

    /**
     * This method performs the following tasks:
     * - Retrieves and processes variable references found in the `condition` of the `KtIfExpression`.
     * - For each branch of the `KtIfExpression` iteratively processes child
     * elements of the branch using `processReferences` to manage variable
     * definitions and update dependency mappings within the branch scope.
     */
    fun KtIfExpression.process() {
      condition?.getVariableReferences()?.forEach {
        addReferences(it, definedVariables)
      }
      val stateSnapshot = definedVariables.copy() // need to be copied due to execution branches
      branches.forEach { branch ->
        val branchState = stateSnapshot.copy()
        branch?.children?.forEach { element ->
          processReferences(element, branchState, outerScopeDefinitions + definedVariables)
        }
      }
    }

    /**
     * Does not make dependencies, but only adds the use of variables and references.
     * This is necessary because loops may not have linear dependencies
     */
    private fun PsiElementToDependencies.searchElements(psiElement: PsiElement) {
      when (psiElement) {
        is KtBinaryExpression -> {
          if (psiElement.operationReference.operationSignTokenType.isAssigment()) {
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

  /**
    Makes a dependency on the element and all of its already initialized references
   */
  private fun PsiElement.addReferences(other: KtReferenceExpression, definedVariables: PsiElementToDependencies) {
    other.reference()?.resolve()?.let { referenceTo ->
      addDependency(referenceTo)
      definedVariables[referenceTo]?.forEach {
        addDependency(it)
      }
    }
  }


  override fun PsiElement.addDependency(other: PsiElement) {
    dependenciesBackward.addIfAbsent(this, other)
    dependenciesForward.addIfAbsent(other, this)
  }
}
