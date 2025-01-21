package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.FunctionSignaturesProvider
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.aiHints.python.PyHintsUtils.functions
import com.jetbrains.edu.aiHints.python.PyHintsUtils.generateSignature

class PyFunctionSignaturesProvider : FunctionSignaturesProvider {
  override fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> {
    return psiFile.functions().mapNotNull { it.generateSignature(signatureSource) }
  }
}