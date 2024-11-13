package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IntroduceWhenSubjectInspection
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.scripting.definitions.runReadAction

class IntroduceWhenSubjectInspectionProcessing(private val project: Project, private val element: KtWhenExpression) :
  InspectionProcessing {
  private val inspection = IntroduceWhenSubjectInspection()

  override fun isApplicable(): Boolean = runReadAction {
    if (!element.isValid) return@runReadAction false
    inspection.isApplicable(element)
  }

  override fun apply() {
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      inspection.applyTo(element, project)
    })
  }

  override fun applyInspection(promptToCode: PromptToCodeResponse, psiFile: PsiFile): PromptToCodeResponse {
    if (!isApplicable()) return promptToCode
    apply()
    return updatePromptToCodeWithoutChangingLines(promptToCode, psiFile)
  }
}
