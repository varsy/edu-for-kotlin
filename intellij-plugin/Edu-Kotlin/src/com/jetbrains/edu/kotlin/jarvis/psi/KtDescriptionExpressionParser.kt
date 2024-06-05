package com.jetbrains.edu.kotlin.jarvis.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.jarvis.DescriptionExpressionParser
import com.jetbrains.edu.jarvis.JarvisDslPackageCallChecker
import com.jetbrains.edu.kotlin.jarvis.utils.DESCRIPTION
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression
import org.jetbrains.kotlin.psi.KtCallExpression

class KtDescriptionExpressionParser : DescriptionExpressionParser {
  override fun parseDescriptionExpression(descriptionExpression: PsiElement): DescriptionExpression? {
    if (!isDescriptionBlock(descriptionExpression) ||
        descriptionExpression !is KtCallExpression ||
        existsNestedDescriptionExpressions(descriptionExpression))
      return null
    return DescriptionExpression(
      descriptionExpression.valueArguments.firstOrNull()?.text ?: "",
      descriptionExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()?.bodyExpression?.text ?: ""
    )
  }

  private fun existsNestedDescriptionExpressions(descriptionExpression: KtCallExpression) =
    PsiTreeUtil.findChildrenOfType(descriptionExpression, KtCallExpression::class.java).any { isDescriptionBlock(it) }

  private fun isDescriptionBlock(element: PsiElement) = element.text.startsWith(DESCRIPTION) &&
                      JarvisDslPackageCallChecker.isCallFromJarvisDslPackage(element, element.language)
}
