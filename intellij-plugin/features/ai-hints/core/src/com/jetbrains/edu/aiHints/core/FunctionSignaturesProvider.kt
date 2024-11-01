package com.jetbrains.edu.aiHints.core

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource

interface FunctionSignaturesProvider {
  fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature>

  companion object {
    private val EP_NAME = LanguageExtension<FunctionSignaturesProvider>("aiHints.functionSignaturesProvider")

    @RequiresReadLock
    fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource, language: Language): List<FunctionSignature> {
      val functionSignaturesProvider = EP_NAME.forLanguage(language) ?: error("${EP_NAME.name} is not implemented for $language")
      return functionSignaturesProvider.getFunctionSignatures(psiFile, signatureSource)
    }
  }
}