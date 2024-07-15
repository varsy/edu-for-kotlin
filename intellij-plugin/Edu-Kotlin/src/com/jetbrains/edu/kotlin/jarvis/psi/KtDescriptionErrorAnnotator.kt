package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import kotlin.reflect.KClass

class KtDescriptionErrorAnnotator : DescriptionErrorAnnotator<KClass<out PsiElement>> {
  override fun <T> getVisibleEntities(
    context: PsiElement,
    vararg targetClasses: KClass<out PsiElement>,
    toEntityOrNull: (PsiElement) -> T?
  ): MutableSet<T> =
    // TODO: Get functions and variables from the whole project, not just the containing file.
    targetClasses.map { PsiTreeUtil.collectElementsOfType(context.containingFile, it.java) }.flatten().mapNotNull { toEntityOrNull(it) }
      .toMutableSet()

  override fun PsiElement.isRelevant() = isDescriptionBlock()

  override fun PsiElement.toNamedFunctionOrNull(): NamedFunction? {
    if (this !is KtNamedFunction) return null
    val functionName = name ?: return null
    // TODO: Handle the case with default arguments
    val numberOfParameters = getChildOfType<KtParameterList>()?.parameters?.size ?: 0
    return NamedFunction(functionName, numberOfParameters)
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

  override fun getDescriptionContentOrNull(element: PsiElement): PsiElement? = element.getChildOfType<KtValueArgumentList>()
}
