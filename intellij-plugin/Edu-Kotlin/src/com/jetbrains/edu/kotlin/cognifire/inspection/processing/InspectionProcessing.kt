package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.psi.PsiFile
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

interface InspectionProcessing {
  fun isApplicable(): Boolean
  fun apply()
  fun applyInspection(promptToCode: PromptToCodeContent, psiFile: PsiFile): PromptToCodeContent
}
