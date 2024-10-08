package com.jetbrains.edu.kotlin.learning.eduAssistant.psi.context

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.util.containers.addIfNotNull
import com.jetbrains.edu.learning.eduAssistant.context.FunctionParameter
import com.jetbrains.edu.learning.eduAssistant.context.FunctionSignature
import com.jetbrains.edu.learning.eduAssistant.context.SignatureSource
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignaturesProvider
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContentAndGetResult
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext

class KtFunctionSignaturesProvider : FunctionSignaturesProvider {

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

}

fun KtNamedFunction.generateSignature(signatureSource: SignatureSource): FunctionSignature? {
  val context = analyzeWithContentAndGetResult()
  context.bindingContext[BindingContext.FUNCTION, this]?.let { functionDescriptor ->
    return FunctionSignature(
      functionDescriptor.name.toString(),
      functionDescriptor.valueParameters.map { FunctionParameter(it.name.toString(), it.type.toString()) },
      functionDescriptor.returnType.toString(),
      signatureSource,
      bodyExpression?.text?.split(System.lineSeparator())?.size
    )
  }
  return null
}
