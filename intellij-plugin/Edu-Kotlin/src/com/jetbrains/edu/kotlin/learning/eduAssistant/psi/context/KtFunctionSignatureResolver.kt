package com.jetbrains.edu.kotlin.learning.eduAssistant.psi.context

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignatureResolver
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtFunctionSignatureResolver : FunctionSignatureResolver {
  override fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? {
    var function: KtNamedFunction? = null
    psiFile.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        when (element) {
          is KtNamedFunction -> {
            // TODO: function parameters are not compared, because otherwise the case with changing function parameters will not work
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
}