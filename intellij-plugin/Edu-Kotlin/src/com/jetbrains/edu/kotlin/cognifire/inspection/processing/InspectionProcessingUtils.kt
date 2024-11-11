package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression

fun updatePromptToCodeWithoutChangingLines(promptToCode: PromptToCodeContent, psiFile: PsiFile) = runReadAction {
  val function = PsiTreeUtil.collectElementsOfType(psiFile, KtFunction::class.java).firstOrNull() ?: error("Cannot find function")
  val functionLines = function.bodyExpression?.children?.joinToString(System.lineSeparator()) { it.text }?.lines() ?: error("Cannot find function body")
  promptToCode.mapIndexed { index, generatedCodeLine ->
    generatedCodeLine.copy(generatedCodeLine = functionLines[index].trim() )
  }
}

fun KtIfExpression.findIfStatementInResponse(promptToCode: PromptToCodeContent) = runReadAction {
  val ifStatement = text.split(System.lineSeparator())
  ifStatement.mapNotNull { ifLine ->
    promptToCode.find { it.generatedCodeLine.contains(ifLine) }
  }.distinctBy { it.promptLineNumber }
}

fun KtExpression.getGeneratedCodeLine(promptToCode: PromptToCodeContent) = runReadAction {
  promptToCode.firstOrNull { it.generatedCodeLine.contains(text) }
}

fun KtIfExpression.getFirstPromptLineNumber(promptToCode: PromptToCodeContent) = runReadAction {
  findIfStatementInResponse(promptToCode).minOfOrNull { it.promptLineNumber }
}

fun KtIfExpression.getLastPromptLineNumber(promptToCode: PromptToCodeContent): Int? = runReadAction {
  findIfStatementInResponse(promptToCode).maxOfOrNull { it.promptLineNumber }
}

fun KtIfExpression.getFirstCodeLineNumber(promptToCode: PromptToCodeContent) = runReadAction {
  findIfStatementInResponse(promptToCode).minOfOrNull { it.codeLineNumber }
}

fun findExpression(psiFile: PsiFile, expression: String) = runReadAction {
  psiFile.text.split(System.lineSeparator()).firstOrNull { it.contains(expression) }?.trim()
         ?: error("Cannot find expression")
}
