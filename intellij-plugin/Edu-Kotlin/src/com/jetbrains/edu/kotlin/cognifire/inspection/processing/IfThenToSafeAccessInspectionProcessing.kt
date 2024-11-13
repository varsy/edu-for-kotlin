package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToSafeAccessInspection
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.scripting.definitions.runReadAction

class IfThenToSafeAccessInspectionProcessing(project: Project, element: KtIfExpression) : IfThenInspectionProcessing(project, element) {

  override val inspection = IfThenToSafeAccessInspection()

  override fun getReplacedExpressionText(psiFile: PsiFile, condition: KtNameReferenceExpression) = runReadAction {
    PsiTreeUtil.collectElementsOfType(psiFile, KtSafeQualifiedExpression::class.java).toList()
      .firstOrNull {
        (it.receiverExpression as? KtNameReferenceExpression)?.getReferencedName() == condition.getReferencedName()
      }?.text
  }
}
