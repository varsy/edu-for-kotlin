package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable
import com.jetbrains.edu.kotlin.jarvis.utils.isDescriptionBlock
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import kotlin.reflect.KClass

class KtDescriptionErrorAnnotator : DescriptionErrorAnnotator {

  override fun PsiElement.isRelevant() = isDescriptionBlock()

  override fun PsiElement.toNamedFunctionOrNull(): NamedFunction? {
    if (this !is KtNamedFunction) return null
    val functionName = name ?: return null
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
