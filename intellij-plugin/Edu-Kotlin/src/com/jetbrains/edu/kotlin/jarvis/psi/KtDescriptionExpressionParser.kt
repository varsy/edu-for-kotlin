package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.kotlin.jarvis.DescriptionRunLineMarkerContributor.Companion.DESCRIPTION
import com.jetbrains.edu.kotlin.jarvis.psi.KtJarvisDslPackageCallChecker.Companion.isCallFromJarvisDslPackage
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import org.jetbrains.kotlin.psi.KtCallExpression

class KtDescriptionExpressionParser : DescriptionExpressionParser {
  override fun parseDescriptionExpression(descriptionExpression: PsiElement): DescriptionExpression? {
    if (descriptionExpression is KtCallExpression) {
      if (existsNestedDescriptionExpressions(descriptionExpression)) return null
      return DescriptionExpression(
        descriptionExpression.valueArguments.first().text,
        descriptionExpression.lambdaArguments.first().text
      )
    }
    return null
  }

  private fun existsNestedDescriptionExpressions(descriptionExpression: KtCallExpression) =
    PsiTreeUtil.findChildrenOfType(descriptionExpression, KtCallExpression::class.java).any {
      it.calleeExpression?.text == DESCRIPTION &&
      isCallFromJarvisDslPackage(it)
    }
}
