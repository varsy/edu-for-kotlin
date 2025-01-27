package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiDebugging.core.slicing.SliceManager

class KSliceManager : SliceManager {

  @Suppress("unused")
  override fun processSlice(psiElement: PsiElement, document: Document, psiFile: PsiFile) {
    val controlDependency = ControlDependency(psiElement).dependenciesForward
    // TODO()
  }

}
