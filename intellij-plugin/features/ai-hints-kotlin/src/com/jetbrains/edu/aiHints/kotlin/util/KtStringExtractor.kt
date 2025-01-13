package com.jetbrains.edu.aiHints.kotlin.util

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.aiHints.kotlin.util.KtFunctionSignaturesProvider.generateSignature
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

object KtStringExtractor {
  fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings {
    val signatureToStrings = psiFile.findAllFunctions().mapNotNull { function ->
      val signature = function.generateSignature(SignatureSource.MODEL_SOLUTION) ?: return@mapNotNull null
      val stringTemplateExpressions = function.collectStringTemplateExpressions()
      val referredStringValues = function.collectReferredStringValues()
      val allStrings = (stringTemplateExpressions + referredStringValues).distinct()
      signature to allStrings
    }.toMap()
    return FunctionsToStrings(signatureToStrings)
  }

  private fun PsiFile.findAllFunctions(): Collection<KtNamedFunction> =
    PsiTreeUtil.findChildrenOfType(this, KtNamedFunction::class.java)

  private fun KtNamedFunction.collectStringTemplateExpressions(): List<String> =
    PsiTreeUtil.findChildrenOfType(this, KtStringTemplateExpression::class.java)
      .map { it.text }

  private fun KtNamedFunction.collectReferredStringValues(): List<String> =
    PsiTreeUtil.findChildrenOfType(this, KtReferenceExpression::class.java)
      .asSequence()
      .mapNotNull { mainReference?.resolve() as? KtProperty }
      .mapNotNull { it.initializer as? KtStringTemplateExpression }
      .mapTo(mutableListOf()) { it.text }
}