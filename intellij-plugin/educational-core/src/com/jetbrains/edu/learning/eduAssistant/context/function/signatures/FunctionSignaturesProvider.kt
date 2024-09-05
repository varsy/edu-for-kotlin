package com.jetbrains.edu.learning.eduAssistant.context.function.signatures

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiFile
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignature
import com.jetbrains.edu.learning.courseFormat.eduAssistant.SignatureSource

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