package com.jetbrains.edu.jarvis

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.ThreadingAssertions

/**
 * Applies the code from the draft block to the main code.
 * Called when all errors are fixed and there are no other nested blocks.
 * [PsiElement] - draft block element.
 */
interface DraftApplier {

  fun applyCodeDraftToMainCode(project: Project, element: PsiElement, psiFile: PsiFile?)

  companion object {
    private val EP_NAME = LanguageExtension<DraftApplier>("Educational.draftApplier")

    fun applyCodeDraftToMainCode(project: Project, element: PsiElement, psiFile: PsiFile?, language: Language) {
      ThreadingAssertions.assertReadAccess()
      EP_NAME.forLanguage(language)?.applyCodeDraftToMainCode(project, element, psiFile)
    }
  }
}
