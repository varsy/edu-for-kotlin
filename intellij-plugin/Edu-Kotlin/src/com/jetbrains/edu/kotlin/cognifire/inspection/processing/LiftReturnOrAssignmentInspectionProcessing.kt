package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse
import org.jetbrains.kotlin.idea.inspections.LiftReturnOrAssignmentInspection
import org.jetbrains.kotlin.idea.inspections.LiftReturnOrAssignmentInspection.Util.LiftType.LIFT_ASSIGNMENT_OUT
import org.jetbrains.kotlin.idea.k2.refactoring.util.BranchedFoldingUtils
import org.jetbrains.kotlin.idea.inspections.LiftReturnOrAssignmentInspection.Util.LiftType.LIFT_RETURN_OUT
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.scripting.definitions.runReadAction

class LiftReturnOrAssignmentInspectionProcessing(private val project: Project, private val element: KtExpression) : InspectionProcessing {

  override fun isApplicable(): Boolean = runReadAction {
    if (!element.isValid) return@runReadAction false
    if (element !is KtIfExpression && element !is KtWhenExpression && element !is KtTryExpression) return@runReadAction false
    val state = LiftReturnOrAssignmentInspection.Util.getState(element, skipLongExpressions = false) ?: return@runReadAction false
    state.any { (it.liftType == LIFT_RETURN_OUT || it.liftType == LIFT_ASSIGNMENT_OUT) && it.isSerious }
  }

  override fun apply() {
    val state = runReadAction { LiftReturnOrAssignmentInspection.Util.getState(element, skipLongExpressions = false) }
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      if (state?.any { it.liftType == LIFT_RETURN_OUT } == true) {
        BranchedFoldingUtils.foldToReturn(element)
      } else if (state?.any { it.liftType == LIFT_ASSIGNMENT_OUT} == true) {
        BranchedFoldingUtils.tryFoldToAssignment(element)
      }
    })
  }

  override fun applyInspection(promptToCode: PromptToCodeResponse, psiFile: PsiFile): PromptToCodeResponse {
    if (!isApplicable()) return promptToCode
    apply()
    return updatePromptToCodeWithoutChangingLines(promptToCode, psiFile)
  }
}
