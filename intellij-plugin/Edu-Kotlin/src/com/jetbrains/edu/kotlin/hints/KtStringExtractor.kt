package com.jetbrains.edu.kotlin.hints

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.learning.hints.context.FunctionsToStrings
import com.jetbrains.edu.learning.hints.context.SignatureSource
import com.jetbrains.edu.learning.hints.StringExtractor
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class KtStringExtractor : StringExtractor {

  override fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings = FunctionsToStrings(
    PsiTreeUtil.findChildrenOfType(psiFile, KtNamedFunction::class.java).mapNotNull { function ->
      function.generateSignature(SignatureSource.MODEL_SOLUTION)?.let { signature ->
        val stringExceptions = PsiTreeUtil.findChildrenOfType(function, KtStringTemplateExpression::class.java).map { it.text }
        val references = PsiTreeUtil.findChildrenOfType(function, KtReferenceExpression::class.java)
        val referredStringValues = references.mapNotNull { reference ->
          (reference.mainReference.resolve() as? KtProperty)?.takeIf { it.initializer is KtStringTemplateExpression }?.initializer?.text
        }
        val allStrings = (stringExceptions + referredStringValues).distinct()
        signature to allStrings
      }
    }.toMap())
}
