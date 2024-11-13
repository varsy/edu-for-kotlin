package com.jetbrains.edu.kotlin.cognifire.inspection

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.kotlin.cognifire.inspection.processing.*
import com.jetbrains.educational.ml.cognifire.responses.GeneratedCodeLine
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtWhenExpression

enum class SupportedInspections {
  CASCADE_IF,
  FOLD_INITIALIZER_AND_IF_TO_ELVIS,
  IF_THEN_TO_ELVIS,
  IF_THEN_TO_SAFE_ACCESS,
  INTRODUCE_WHEN_SUBJECT,
  JOIN_DECLARATION_AND_ASSIGNMENT,
  LIFT_RETURN_OR_ASSIGNMENT;

  fun applyInspection(project: Project, element: PsiElement, psiFile: PsiFile, promptToCode: List<GeneratedCodeLine>): List<GeneratedCodeLine> =
    when (element) {
      is KtIfExpression -> when (this) {
        CASCADE_IF -> CascadeIfInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        FOLD_INITIALIZER_AND_IF_TO_ELVIS -> FoldInitializerAndIfToElvisInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        IF_THEN_TO_ELVIS -> IfThenToElvisInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        IF_THEN_TO_SAFE_ACCESS -> IfThenToSafeAccessInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        LIFT_RETURN_OR_ASSIGNMENT -> LiftReturnOrAssignmentInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        else -> promptToCode
      }
      is KtWhenExpression -> when (this) {
        INTRODUCE_WHEN_SUBJECT -> IntroduceWhenSubjectInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        LIFT_RETURN_OR_ASSIGNMENT -> LiftReturnOrAssignmentInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        else -> promptToCode
      }
      is KtTryExpression -> when (this) {
        LIFT_RETURN_OR_ASSIGNMENT -> LiftReturnOrAssignmentInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        else -> promptToCode
      }
      is KtProperty -> when (this) {
        JOIN_DECLARATION_AND_ASSIGNMENT -> JoinDeclarationAndAssignmentInspectionProcessing(project, element).applyInspection(promptToCode, psiFile)
        else -> promptToCode
      }
      else -> promptToCode
    }
}
