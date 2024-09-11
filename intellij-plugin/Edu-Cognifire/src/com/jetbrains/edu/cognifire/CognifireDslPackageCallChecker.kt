package com.jetbrains.edu.cognifire

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * Checks if a given [PsiElement] represents a call from the Cognifire DSL package.
 */
interface CognifireDslPackageCallChecker {
  fun isCallFromCognifireDslPackage(element: PsiElement): Boolean

  companion object {
    private val EP_NAME = LanguageExtension<CognifireDslPackageCallChecker>("Educational.cognifireDslPackageCallChecker")

    fun isCallFromCognifireDslPackage(element: PsiElement, language: Language): Boolean {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.isCallFromCognifireDslPackage(element)
      ?: error("Not supported to provide a `code` expression for the ${language.displayName} language")
    }
  }
}
