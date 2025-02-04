package com.jetbrains.edu.aiHints.kotlin.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.edu.aiHints.core.api.FunctionSignaturesManager
import com.jetbrains.edu.aiHints.core.context.FunctionParameter
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.symbols.name
import org.jetbrains.kotlin.psi.KtNamedFunction

object KtFunctionSignaturesManager : FunctionSignaturesManager {
  override fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? {
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

  override fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> {
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

  internal fun KtNamedFunction.generateSignature(signatureSource: SignatureSource): FunctionSignature? = runReadAction {
    val ktFunction = this

    analyze(ktFunction) {
      val kaFunction = ktFunction.symbol
      val name = kaFunction.name ?: return@runReadAction null

      FunctionSignature(
        name.toString(),
        kaFunction.valueParameters.map {
          FunctionParameter(
            it.name.toString(),
            it.returnType.toString()
          )
        },
        kaFunction.returnType.toString(),
        signatureSource,
        ktFunction.bodyExpression?.text?.lines()?.size
      )
    }
  }
}