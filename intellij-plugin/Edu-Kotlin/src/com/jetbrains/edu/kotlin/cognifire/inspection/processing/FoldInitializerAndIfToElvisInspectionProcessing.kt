package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse.GeneratedCodeLine
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import org.jetbrains.kotlin.idea.inspections.FoldInitializerAndIfToElvisInspection
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.scripting.definitions.runReadAction
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

class FoldInitializerAndIfToElvisInspectionProcessing(private val project: Project, private val element: KtIfExpression)
  : InspectionProcessing {
  private val inspection = FoldInitializerAndIfToElvisInspection()

  override fun isApplicable(): Boolean = runReadAction {
    if (!element.isValid) return@runReadAction false
    inspection.isApplicable(element)
  }

  override fun apply() {
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      inspection.applyTo(element, project)
    })
  }

  override fun applyInspection(promptToCode: PromptToCodeContent, psiFile: PsiFile): PromptToCodeContent {
    if (!isApplicable()) return promptToCode
    val variableDeclaration = element.siblings(forward = false, withItself = false)
      .firstIsInstanceOrNull<KtExpression>() as? KtVariableDeclaration
    val variable = variableDeclaration?.let { variable ->
      promptToCode.find { it.generatedCodeLine.contains(runReadAction { variable.text }) }
    } ?: return promptToCode
    val ifPromptLines = element.findIfStatementInResponse(promptToCode)
    val lastIfLine = element.getLastPromptLineNumber(promptToCode) ?: return promptToCode

    apply()

    val variableDeclarationText = runReadAction { variableDeclaration.text }
    val newPromptToCode = mutableListOf<GeneratedCodeLine>()
    promptToCode.filter { it.promptLineNumber < variable.promptLineNumber }.forEach {
      newPromptToCode.add(it.copy())
    }
    promptToCode.firstOrNull { it.promptLineNumber == variable.promptLineNumber }?.let {
      newPromptToCode.add(it.copy(generatedCodeLine = variableDeclarationText))
    }
    ifPromptLines.forEach {
      newPromptToCode.add(it.copy(codeLineNumber = variable.codeLineNumber, generatedCodeLine = variableDeclarationText))
    }
    var codeLineNumber = variable.codeLineNumber + 1
    promptToCode.filter { it.promptLineNumber > lastIfLine }.forEach {
      newPromptToCode.add(it.copy(codeLineNumber = codeLineNumber))
      codeLineNumber++
    }
    return newPromptToCode
  }
}
