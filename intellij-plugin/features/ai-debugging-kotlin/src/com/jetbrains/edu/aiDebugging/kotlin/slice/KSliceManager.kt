package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.aiDebugging.core.slicing.SliceManager
import org.jetbrains.kotlin.psi.KtFunction

class KSliceManager : SliceManager {

  @Suppress("unused")
  override fun processSlice(psiElement: PsiElement, document: Document, psiFile: PsiFile) {
    // TODO make logic for finding KtFunction
    val dependencies = PsiTreeUtil.findChildrenOfType(psiElement, KtFunction::class.java)
      .map { FunctionControlDependency(it) }
    // TODO()
  }

}
