package com.jetbrains.edu.jarvis

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * Applies the code from the code block to the main code.
 * Called when all errors are fixed and there are no other nested blocks.
 * [PsiElement] - code block element.
 */
interface CodeApplier {

  fun applyDraftCodeToMainCode(project: Project, element: PsiElement, psiFile: PsiFile?)

  companion object {
    private val EP_NAME = LanguageExtension<CodeApplier>("Educational.codeApplier")

    fun applyDraftCodeToMainCode(project: Project, element: PsiElement, psiFile: PsiFile?, language: Language) {
      ThreadingAssertions.assertReadAccess()
      EP_NAME.forLanguage(language)?.applyDraftCodeToMainCode(project, element, psiFile)
    }
  }
}
