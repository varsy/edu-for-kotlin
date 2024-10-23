package com.jetbrains.edu.learning.hints

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.learning.hints.context.FunctionSignature
import com.jetbrains.edu.learning.hints.context.SignatureSource

interface FunctionSignaturesProvider {
  fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature>

  companion object {
    private val EP_NAME = LanguageExtension<FunctionSignaturesProvider>("Educational.functionSignaturesProvider")

    @RequiresReadLock
    fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource, language: Language): List<FunctionSignature> {
      val functionSignaturesProvider = EP_NAME.forLanguage(language) ?: error("$EP_NAME is not implemented for $language")
      return functionSignaturesProvider.getFunctionSignatures(psiFile, signatureSource)
    }
  }
}