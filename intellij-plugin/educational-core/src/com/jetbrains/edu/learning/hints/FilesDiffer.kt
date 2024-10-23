package com.jetbrains.edu.learning.hints

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock

interface FilesDiffer {
  fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean): List<String>

  companion object {
    private val EP_NAME = LanguageExtension<FilesDiffer>("Educational.filesDiffer")

    @RequiresReadLock
    fun findDifferentMethods(before: PsiFile, after: PsiFile, language: Language, considerParameters: Boolean = false): List<String> {
      val filesDiffer = EP_NAME.forLanguage(language) ?: error("$EP_NAME is not implemented for $language")
      return filesDiffer.findChangedMethods(before, after, considerParameters)
    }
  }
}