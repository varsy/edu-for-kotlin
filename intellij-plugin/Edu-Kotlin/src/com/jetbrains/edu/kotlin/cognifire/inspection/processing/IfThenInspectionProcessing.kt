package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse.GeneratedCodeLine
import org.jetbrains.kotlin.idea.codeinsight.api.classic.inspections.AbstractApplicabilityBasedInspection
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.scripting.definitions.runReadAction

abstract class IfThenInspectionProcessing(private val project: Project, private val element: KtIfExpression) : InspectionProcessing {
  abstract val inspection: AbstractApplicabilityBasedInspection<KtIfExpression>

  override fun isApplicable(): Boolean = runReadAction {
    if (!element.isValid) return@runReadAction false
    inspection.isApplicable(element)
  }

  override fun apply() {
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      inspection.applyTo(element, project)
    })
  }

  abstract fun getReplacedExpressionText(psiFile: PsiFile, condition: KtNameReferenceExpression): String?

  override fun applyInspection(promptToCode: PromptToCodeContent, psiFile: PsiFile): PromptToCodeContent {
    if (!isApplicable()) return promptToCode
    val ifPromptLines = element.findIfStatementInResponse(promptToCode)
    val firstIfPromptLine = element.getFirstPromptLineNumber(promptToCode) ?: return promptToCode
    val firstIfCodeLine = element.getFirstCodeLineNumber(promptToCode) ?: return promptToCode
    val lastIfPromptLine = element.getLastPromptLineNumber(promptToCode) ?: return promptToCode
    val condition = (element.condition as? KtBinaryExpression)?.left as? KtNameReferenceExpression ?: return promptToCode

    apply()

    val replacedExpressionText = getReplacedExpressionText(psiFile, condition) ?: return promptToCode
    val expression = findExpression(psiFile, replacedExpressionText)
    val newPromptToCode = mutableListOf<GeneratedCodeLine>()
    promptToCode.filter { it.promptLineNumber < firstIfPromptLine }.forEach {
      newPromptToCode.add(it.copy())
    }
    ifPromptLines.forEach {
      newPromptToCode.add(it.copy(codeLineNumber = firstIfCodeLine, generatedCodeLine = expression))
    }
    var codeLineNumber = firstIfCodeLine + 1
    promptToCode.filter { it.promptLineNumber > lastIfPromptLine }.forEach {
      newPromptToCode.add(it.copy(codeLineNumber = codeLineNumber))
      codeLineNumber++
    }
    return newPromptToCode
  }
}
