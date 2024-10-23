package com.jetbrains.edu.learning.hints

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

interface FunctionSignatureResolver {

  fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement?

  companion object {
    private val EP_NAME = LanguageExtension<FunctionSignatureResolver>("Educational.functionSignatureResolver")

    fun getFunctionBySignature(psiFile: PsiFile, functionName: String, language: Language): PsiElement? {
      ApplicationManager.getApplication().assertReadAccessAllowed()
      return EP_NAME.forLanguage(language)?.getFunctionBySignature(psiFile, functionName)
    }
  }
}