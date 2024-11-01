package com.jetbrains.edu.aiHints.core

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface FunctionSignatureResolver {
  fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement?

  companion object {
    private val EP_NAME = LanguageExtension<FunctionSignatureResolver>("aiHints.functionSignatureResolver")

    @RequiresReadLock
    fun getFunctionBySignature(psiFile: PsiFile, functionName: String, language: Language): PsiElement? {
      val functionSignatureResolver = EP_NAME.forLanguage(language) ?: error("${EP_NAME.name} is not implemented for $language")
      return functionSignatureResolver.getFunctionBySignature(psiFile, functionName)
    }
  }
}