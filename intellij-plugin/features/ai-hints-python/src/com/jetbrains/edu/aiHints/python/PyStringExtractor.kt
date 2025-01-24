package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.StringExtractor
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.aiHints.python.PyHintsUtils.functions
import com.jetbrains.edu.aiHints.python.PyHintsUtils.generateSignature
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression

class PyStringExtractor : StringExtractor {
  override fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings = psiFile.functions().mapNotNull { pyFunction ->
    val signature = pyFunction.generateSignature(SignatureSource.MODEL_SOLUTION) ?: return@mapNotNull null
    val strings = pyFunction.referredStringValues() + pyFunction.stringLiteralExpressions()
    signature to strings
  }.toMap().let(::FunctionsToStrings)

  private fun PyFunction.stringLiteralExpressions(): List<String> =
    PsiTreeUtil.findChildrenOfType(this, PyStringLiteralExpression::class.java)
      .mapNotNull { it.stringValue }

  private fun PyFunction.referredStringValues(): List<String> =
    PsiTreeUtil.findChildrenOfType(this, PyReferenceExpression::class.java)
      .mapNotNull {
        it.reference.resolve().asSafely<PyTargetExpression>()?.findAssignedValue().asSafely<PyStringLiteralExpression>()?.stringValue
      }
}