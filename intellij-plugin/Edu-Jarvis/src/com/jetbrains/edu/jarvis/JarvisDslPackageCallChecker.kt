package com.jetbrains.edu.jarvis

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * Checks if a given [PsiElement] represents a call from the Jarvis DSL package.
 */
interface JarvisDslPackageCallChecker {
  fun isCallFromJarvisDslPackage(element: PsiElement): Boolean

  companion object {
    private val EP_NAME = LanguageExtension<JarvisDslPackageCallChecker>("Educational.jarvisDslPackageCallChecker")

    fun isCallFromJarvisDslPackage(element: PsiElement, language: Language): Boolean {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.isCallFromJarvisDslPackage(element)
      ?: error("Not supported to provide a draft expression for the ${language.displayName} language")
    }
  }
}