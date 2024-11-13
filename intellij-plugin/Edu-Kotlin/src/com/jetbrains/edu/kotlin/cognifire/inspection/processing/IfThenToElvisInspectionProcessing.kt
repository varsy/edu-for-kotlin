package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.inspections.branchedTransformations.IfThenToElvisInspection
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.scripting.definitions.runReadAction

class IfThenToElvisInspectionProcessing(project: Project, element: KtIfExpression) : IfThenInspectionProcessing(project, element) {

  override val inspection = IfThenToElvisInspection()

  private fun KtExpression.getNameReferenceExpression(): KtNameReferenceExpression? {
    if (this is KtNameReferenceExpression) return this
    return PsiTreeUtil.getChildOfType(this, KtNameReferenceExpression::class.java)
  }

  override fun getReplacedExpressionText(psiFile: PsiFile, condition: KtNameReferenceExpression) = runReadAction {
    PsiTreeUtil.collectElementsOfType(psiFile, KtBinaryExpression::class.java).toList()
      .firstOrNull {
        it.left?.getNameReferenceExpression()?.getReferencedName() == condition.getReferencedName() &&
        it.operationToken.debugName == "ELVIS"
      }?.text
  }
}
