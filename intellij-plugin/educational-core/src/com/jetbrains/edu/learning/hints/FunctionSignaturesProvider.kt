package com.jetbrains.edu.learning.hints

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.hints.context.FunctionSignature
import com.jetbrains.edu.learning.hints.context.SignatureSource

interface FunctionSignaturesProvider {

  fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature>

  companion object {
    private val EP_NAME = LanguageExtension<FunctionSignaturesProvider>("Educational.functionSignaturesProvider")

    fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource, language: Language): List<FunctionSignature> {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.getFunctionSignatures(psiFile, signatureSource) ?: emptyList()
    }
  }
}