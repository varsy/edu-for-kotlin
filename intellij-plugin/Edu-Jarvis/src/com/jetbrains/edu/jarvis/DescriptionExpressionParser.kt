package com.jetbrains.edu.jarvis

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.psi.PsiElement
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.edu.learning.courseFormat.jarvis.DescriptionExpression

interface DescriptionExpressionParser {
  fun parseDescriptionExpression(descriptionExpression: PsiElement): DescriptionExpression?

  companion object {
    private val EP_NAME = LanguageExtension<DescriptionExpressionParser>("Educational.descriptionExpressionParser")

    fun parseDescriptionExpression(descriptionExpression: PsiElement, language: Language): DescriptionExpression? {
      ThreadingAssertions.assertReadAccess()
      return EP_NAME.forLanguage(language)?.parseDescriptionExpression(descriptionExpression)
    }
  }
}
