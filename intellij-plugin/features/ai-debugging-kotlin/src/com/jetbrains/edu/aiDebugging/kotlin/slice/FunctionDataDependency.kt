package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.aiDebugging.core.slicing.PsiElementToDependencies
import org.jetbrains.kotlin.idea.completion.reference
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression

class FunctionDataDependency(psiElement: PsiElement) {

  val dependenciesForward = mutableMapOf<PsiElement, HashSet<PsiElement>>()
  val dependenciesBackward = mutableMapOf<PsiElement, HashSet<PsiElement>>()

  init {
    processReferences(psiElement, mutableMapOf<PsiElement, HashSet<PsiElement>>())
  }


  private fun processReferences(
    psiElement: PsiElement,
    definedVariables: PsiElementToDependencies,
    outerScopeDefinitions: List<PsiElementToDependencies> = emptyList()
  ) {
    when (psiElement) {

      is KtFunction -> {
        psiElement.valueParameterList?.parameters?.forEach {
          definedVariables.addIfAbsent(it)
          outerScopeDefinitions.forEach { dependency -> dependency.addIfAbsent(it) }
        }
        psiElement.bodyBlockExpression?.statements?.forEach {
          processReferences(it, definedVariables, outerScopeDefinitions)
        }
      }

      is KtProperty -> {
        definedVariables.addIfAbsent(psiElement)
        outerScopeDefinitions.forEach { dependency -> dependency.addIfAbsent(psiElement) }
        psiElement.initializer?.let {
          it.getVariableReferences().forEach {
            it.reference()?.resolve()?.let { referenceTo ->
              psiElement.addDependency(referenceTo)
              definedVariables[referenceTo]?.forEach {
                psiElement.addDependency(it)
              }
            }

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
            definedVariables.addIfAbsent(referenceTo, psiElement)
            outerScopeDefinitions.forEach { dependency -> dependency.addIfAbsent(referenceTo, psiElement) }
            psiElement.addDependency(referenceTo)
          }
          psiElement.right?.getVariableReferences()?.forEach {
            it.reference()?.resolve()?.let { referenceTo ->
              psiElement.addDependency(referenceTo)
              definedVariables[referenceTo]?.forEach {
                psiElement.addDependency(it)
              }
            }
          }
        }
      }

      is KtPostfixExpression -> {
        psiElement.baseExpression?.reference()?.resolve()?.let {
          definedVariables.addIfAbsent(it, psiElement)
          outerScopeDefinitions.forEach { dependency -> dependency.addIfAbsent(it, psiElement) }
          psiElement.addDependency(it)
        }
      }

      is KtForExpression -> {
        psiElement.loopParameter?.let {
          definedVariables.addIfAbsent(it)
          outerScopeDefinitions.forEach { dependency -> dependency.addIfAbsent(it) }
        }
        psiElement.loopRange?.getVariableReferences()?.forEach {
          it.reference()?.resolve()?.let { referenceTo ->
            psiElement.addDependency(referenceTo)
            definedVariables[referenceTo]?.forEach {
              psiElement.addDependency(it)
            }
          }
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
          it.reference()?.resolve()?.let { referenceTo ->
            psiElement.addDependency(referenceTo)
            definedVariables[referenceTo]?.forEach {
              psiElement.addDependency(it)
            }
          }
        }
        psiElement.body?.children?.forEach {
          processReferences(it, definedVariables, outerScopeDefinitions)
        }
      }

      is KtCallExpression -> {
        psiElement.valueArgumentList?.arguments?.forEach {
          it.getArgumentExpression()?.getVariableReferences()?.forEach {
            it.reference()?.resolve()?.let { referenceTo ->
              psiElement.addDependency(referenceTo)
              definedVariables[referenceTo]?.forEach {
                psiElement.addDependency(it)
              }
            }
          }
        }
      }

      is KtWhenExpression -> {
        psiElement.subjectExpression?.let {
          if (it is KtReferenceExpression) {
            it.reference()?.resolve()?.let { referenceTo ->
              psiElement.addDependency(referenceTo)
              definedVariables[referenceTo]?.forEach {
                psiElement.addDependency(it)
              }
            }
          }
        }
        val state = definedVariables.copy()
        psiElement.entries.forEach {
          with(it.expression) {
            if (this is KtBlockExpression) {
              val statement = state.copy()
              this.statements.forEach {
                processReferences(it, statement, outerScopeDefinitions + definedVariables)
              }
            }
            else {
              this?.let { processReferences(it, state.copy(), outerScopeDefinitions + definedVariables) }
            }
          }
        }
      }

      is KtIfExpression -> { // TODO add elif expression
        psiElement.condition?.getVariableReferences()?.forEach {
          it.reference()?.resolve()?.let { referenceTo ->
            psiElement.addDependency(referenceTo)
            definedVariables[referenceTo]?.forEach {
              psiElement.addDependency(it)
            }
          }
        }
        val state = definedVariables.copy()
        psiElement.then?.let {
          val brachState = state.copy()
          it.children.forEach {
            processReferences(it, brachState, outerScopeDefinitions + definedVariables)
          }
        }
        psiElement.`else`?.let {
          val brachState = state.copy()
          it.children.forEach {
            processReferences(it, brachState, outerScopeDefinitions + definedVariables)
          }
        }
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
            addIfAbsent(it, psiElement)
            outerScopeDefinitions?.forEach { dependency -> dependency.addIfAbsent(it, psiElement) }
          }
        }
      }

      is KtPostfixExpression -> {
        psiElement.baseExpression?.reference()?.resolve()?.let {
          addIfAbsent(it, psiElement)
          outerScopeDefinitions?.forEach { dependency -> dependency.addIfAbsent(it, psiElement) }
        }
      }
    }
  }

  private fun PsiElement.addDependency(other: PsiElement) {
    dependenciesForward.addIfAbsent(this, other)
    dependenciesBackward.addIfAbsent(other, this)
  }

  private fun PsiElementToDependencies.addAll(from: PsiElement, others: Collection<PsiElement> = emptySet()) {
    getOrPut(from) { hashSetOf() }.addAll(others)
  }

  private fun PsiElementToDependencies.addIfAbsent(from: PsiElement, to: PsiElement? = null) {
    getOrPut(from) { hashSetOf() }.also { collection ->
      to?.let {
        collection.add(it)
      }
    }
  }

  private fun PsiElement.getVariableReferences() =
    if (this is KtReferenceExpression) {
      listOf(this)
    }
    else {
      PsiTreeUtil.findChildrenOfType<KtReferenceExpression>(this, KtReferenceExpression::class.java)
        .filter { it !is KtOperationReferenceExpression }
    }

  private fun KtSingleValueToken?.isChange() = this != null && this.value in operations

  private fun PsiElementToDependencies.copy() = mapValues { it.value.toHashSet() }.toMutableMap()

  companion object {
    private val changeOperations = listOf("+=", "*=", "-=", "/=")
    private val operations = listOf("=") + changeOperations
  }
}