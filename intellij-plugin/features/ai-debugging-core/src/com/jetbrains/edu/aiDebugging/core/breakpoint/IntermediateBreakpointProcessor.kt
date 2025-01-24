package com.jetbrains.edu.aiDebugging.core.breakpoint

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.concurrency.annotations.RequiresReadLock

abstract class IntermediateBreakpointProcessor {

  abstract fun findBreakpointLines(psiElement: PsiElement, document: Document, psiFile: PsiFile): List<Int>

  protected fun Document.getAllReferencesLines(element: PsiElement) =
    ReferencesSearch.search(element).findAll().mapNotNull {
      getLineNumber(it.element)
    }.distinct()

  protected fun Document.getLineNumber(element: PsiElement) = getLineNumber(element.textRange.startOffset)

  companion object {
    private val EP_NAME = LanguageExtension<IntermediateBreakpointProcessor>("aiDebugging.intermediateBreakpointProcessor")

    @RequiresReadLock
    fun calculateIntermediateBreakpointPositions(
      virtualFile: VirtualFile,
      wrongCodeLineNumbers: List<Int>,
      project: Project,
      language: Language
    ): List<Int> {
      val intermediateBreakpointProcessor = EP_NAME.forLanguage(language) ?: error("${EP_NAME.name} is not implemented for $language")
      val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
      val document = psiFile?.viewProvider?.document
      return wrongCodeLineNumbers.map { wrongCodeLineNumber ->
        val psiElement = document?.getPsiElementAtLine(psiFile, wrongCodeLineNumber) ?: return emptyList()
        intermediateBreakpointProcessor.findBreakpointLines(psiElement, document, psiFile)
      }.flatten().distinct()
    }

    private fun Document.getPsiElementAtLine(psiFile: PsiFile, line: Int): PsiElement? {
      val lineStartOffset = getLineStartOffset(line)
      var result: PsiElement? = null
      psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
          if (element.textRange.startOffset >= lineStartOffset && element !is PsiWhiteSpace) {
            result = element
            stopWalking()
          }
          super.visitElement(element)
        }
      })
      return result
    }

  }
}
