package com.jetbrains.edu.cognifire.writers

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.models.BaseProdeExpression

interface ExpressionWriter<T : BaseProdeExpression> {
  fun addExpression(project: Project, element: PsiElement, text: String, oldExpression: T? = null): T?
}
