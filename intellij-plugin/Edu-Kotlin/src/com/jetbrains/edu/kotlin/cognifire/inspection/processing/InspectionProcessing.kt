package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.psi.PsiFile
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

interface InspectionProcessing {
  fun isApplicable(): Boolean
  fun apply()
  fun applyInspection(promptToCode: PromptToCodeResponse, psiFile: PsiFile): PromptToCodeResponse
}
