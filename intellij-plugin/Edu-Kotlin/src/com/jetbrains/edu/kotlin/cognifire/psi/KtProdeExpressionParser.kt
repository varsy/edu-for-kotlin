package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.cognifire.models.ProdeExpression
import com.jetbrains.edu.cognifire.parsers.ProdeExpressionParser
import com.jetbrains.edu.cognifire.utils.isPromptBlock
import org.jetbrains.kotlin.psi.KtCallExpression

class KtProdeExpressionParser : ProdeExpressionParser {
  override fun getProdeExpressions(file: PsiFile): List<ProdeExpression> {
    val response = mutableListOf<ProdeExpression>()

    val callExpressions = PsiTreeUtil.findChildrenOfType(file, KtCallExpression::class.java)
    for (element in callExpressions) {
      if (!element.isPromptBlock()) continue
      val promptExpression = KtPromptExpressionParser().getExpression(element) ?: continue
      val codeElement = ElementSearch.findCodeElement(element) { it.nextSibling } ?: continue
      val codeExpression = KtCodeExpressionParser().getExpression(codeElement) ?: continue
      response.add(ProdeExpression(promptExpression, codeExpression))
    }

    return response
  }

}