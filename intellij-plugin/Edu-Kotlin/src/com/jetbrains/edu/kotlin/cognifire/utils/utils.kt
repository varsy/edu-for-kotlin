package com.jetbrains.edu.kotlin.cognifire.utils

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtValueArgument

const val UNIT_RETURN_VALUE = "Unit"
const val TODO_MARKER = "TODO"
const val EMPTY_TODO = ""
const val QUOTE_CHAR = '"'

fun findBlock(
  element: PsiElement,
  step: (PsiElement) -> PsiElement?,
  condition: (PsiElement) -> Boolean
): PsiElement? {
  var possibleBlock: PsiElement? = element
  while(possibleBlock != null && !condition(possibleBlock)) {
    possibleBlock = step(possibleBlock)
  }
  return possibleBlock
}

fun createPsiFile(project: Project, functionSignature: String, code: String): PsiFile =
  PsiFileFactory.getInstance(project).createFileFromText("fileName.kt", KotlinLanguage.INSTANCE,
    """
      $functionSignature {
            $code
        }
    """.trimIndent()
  )

fun PsiElement.getTodoMessageOrNull() = if (this is KtCallExpression && calleeExpression?.text == TODO_MARKER) {
  valueArguments.firstOrNull()?.getArgumentExpression()?.text ?: EMPTY_TODO
} else {
  null
}

fun KtValueArgument.getBaseContentOffset(): Int {
  val trimmedPromptPromptText = text.trimStart(QUOTE_CHAR).trimStart()
  val trimmedOffset = text.length - trimmedPromptPromptText.length
  return textOffset + trimmedOffset
}
