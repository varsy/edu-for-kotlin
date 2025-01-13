package com.jetbrains.edu.aiHints.kotlin.util

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.edu.aiHints.core.context.FunctionParameter
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContentAndGetResult
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext

object KtFunctionSignaturesProvider {
  fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? {
    var function: KtNamedFunction? = null
    psiFile.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        when (element) {
          is KtNamedFunction -> {
            // Function parameters are not compared, because otherwise the case with changing function parameters will not work
            if (element.name == functionName) {
              function = element
            }
          }
          else -> {
            super.visitElement(element)
          }
        }
      }
    })
    return function
  }

  fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> {
    val functionSignatures = mutableListOf<FunctionSignature>()
    psiFile.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        when (element) {
          is KtNamedFunction -> {
            if (element.name != null) {
              functionSignatures.addIfNotNull(element.generateSignature(signatureSource))
            }
          }
          else -> {
            super.visitElement(element)
          }
        }
      }
    })
    return functionSignatures
  }

  internal fun KtNamedFunction.generateSignature(signatureSource: SignatureSource): FunctionSignature? {
    val context = analyzeWithContentAndGetResult()
    context.bindingContext[BindingContext.FUNCTION, this]?.let { functionDescriptor ->
      return FunctionSignature(
        functionDescriptor.name.toString(),
        functionDescriptor.valueParameters.map {
          FunctionParameter(
            it.name.toString(),
            it.type.toString()
          )
        },
        functionDescriptor.returnType.toString(),
        signatureSource,
        bodyExpression?.text?.split(System.lineSeparator())?.size
      )
    }
    return null
  }
}