package com.jetbrains.edu.aiDebugging.core.slicing

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

typealias PsiElementToDependencies = MutableMap<PsiElement, HashSet<PsiElement>>

interface SliceManager {

  // TODO change return type to Tree<?>
  fun processSlice(psiElement: PsiElement, document: Document, psiFile: PsiFile)

  companion object {
    private val EP_NAME = LanguageExtension<SliceManager>("aiDebugging.sliceManager")

    @Suppress("unused")
    fun getInstance(language: Language): SliceManager = EP_NAME.forLanguage(language)
  }
}
