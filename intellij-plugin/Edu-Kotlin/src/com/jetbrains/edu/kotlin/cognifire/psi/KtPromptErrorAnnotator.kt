package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.cognifire.PromptErrorAnnotator
import com.jetbrains.edu.cognifire.models.NamedFunction
import com.jetbrains.edu.cognifire.models.NamedVariable
import com.jetbrains.edu.kotlin.cognifire.utils.isPromptBlock
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import kotlin.reflect.KClass

class KtPromptErrorAnnotator : PromptErrorAnnotator<KClass<out PsiElement>> {
  override fun <T> getVisibleEntities(
    context: PsiElement,
    vararg targetClasses: KClass<out PsiElement>,
    toEntityOrNull: (PsiElement) -> T?
  ): MutableSet<T> =
    // TODO: Get functions and variables from the whole project, not just the containing file.
    targetClasses.map { PsiTreeUtil.collectElementsOfType(context.containingFile, it.java) }.flatten().mapNotNull { toEntityOrNull(it) }
      .toMutableSet()

  override fun PsiElement.isRelevant() = isPromptBlock()

  override fun PsiElement.toNamedFunctionOrNull() =
    (this as? KtNamedFunction)?.let {
      NamedFunction(
        name ?: return null,
        valueParameters.filter { it.hasDefaultValue() }.size..valueParameters.size,
        valueParameters.map { it.name ?: it.text }
      )
    }

  override fun PsiElement.toNamedVariableOrNull(): NamedVariable? {
    return when (this) {
      is KtProperty -> {
        val variableName = name ?: return null
        NamedVariable(variableName)
      }

      is KtParameter -> {
        val variableName = name ?: return null
        NamedVariable(variableName)
      }

      else -> null
    }
  }

  override fun getNamedVariableClasses(): Array<KClass<out PsiElement>> = arrayOf(KtProperty::class, KtParameter::class)
  override fun getNamedFunctionClasses(): Array<KClass<out PsiElement>> = arrayOf(KtNamedFunction::class)

  override fun getPromptContentOrNull(element: PsiElement): PsiElement? = element.getChildOfType<KtValueArgumentList>()
}
