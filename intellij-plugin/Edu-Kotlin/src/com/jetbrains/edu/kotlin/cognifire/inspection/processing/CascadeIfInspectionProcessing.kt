package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.educational.ml.cognifire.responses.GeneratedCodeLine
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse
import org.jetbrains.kotlin.idea.base.psi.isOneLiner
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfToWhenIntention
import org.jetbrains.kotlin.idea.intentions.branches
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis
import org.jetbrains.kotlin.scripting.definitions.runReadAction

class CascadeIfInspectionProcessing(private val project: Project, private val element: KtIfExpression) :
  InspectionProcessing {
  private val inspection = IfToWhenIntention()

  override fun isApplicable(): Boolean = runReadAction {
    if (!element.isValid) return@runReadAction false
    val branches = element.branches
    if (branches.size <= 2) return@runReadAction false
    if (element.isOneLiner()) return@runReadAction false
    if (branches.any { it == null || it.lastBlockStatementOrThis() is KtIfExpression }) return@runReadAction false
    if (element.anyDescendantOfType<KtExpressionWithLabel> {
        it is KtBreakExpression || it is KtContinueExpression
      }) return@runReadAction false
    true
  }

  override fun apply() {
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      inspection.applyTo(element, null)
    })
  }

  override fun applyInspection(promptToCode: PromptToCodeResponse, psiFile: PsiFile): PromptToCodeResponse {
    if (!isApplicable()) return promptToCode
    val conditionElement = runReadAction {
      ((element.condition as? KtBinaryExpression)?.right ?: element.condition)?.text
    } ?: return promptToCode
    val firstIfPromptLine = element.getFirstPromptLineNumber(promptToCode) ?: return promptToCode
    val lastIfPromptLine = element.getLastPromptLineNumber(promptToCode) ?: return promptToCode

    apply()

    val newPromptToCode = mutableListOf<GeneratedCodeLine>()
    promptToCode.filter { it.promptLineNumber < firstIfPromptLine }.forEach {
      newPromptToCode.add(it.copy())
    }
    val whenExpression = getWhenExpression(psiFile, conditionElement) ?: return promptToCode
    var (codeLineNumber, currentElement) = addWhenBranches(whenExpression, newPromptToCode, firstIfPromptLine, promptToCode, psiFile)
    codeLineNumber = addElseBranchIfNeeded(currentElement, whenExpression, newPromptToCode, codeLineNumber, promptToCode)
    addEndOfCode(newPromptToCode, codeLineNumber, promptToCode, lastIfPromptLine, element.condition)
    return newPromptToCode
  }

  private fun getWhenExpression(psiFile: PsiFile, condition: String) = runReadAction {
    PsiTreeUtil.collectElementsOfType(psiFile, KtWhenExpression::class.java).toList()
      .firstOrNull { it.entries.firstOrNull()?.conditions?.firstOrNull()?.text == condition }
  }

  private fun addWhenBranches(
    whenExpression: KtWhenExpression,
    newPromptToCode: MutableList<GeneratedCodeLine>,
    codeLineNumber: Int,
    promptToCode: PromptToCodeResponse,
    psiFile: PsiFile
  ): Pair<Int, KtIfExpression> = runReadAction {
    var lineNumber = codeLineNumber
    var currentElement = element.copy() as KtIfExpression
    for (i in 0 until currentElement.branches.count()) {
      val conditionLine = currentElement.condition?.getGeneratedCodeLine(promptToCode) ?: error("Cannot find condition")
      val conditionInWhen =
        whenExpression.entries[i]?.conditions?.firstOrNull()?.text ?: error("Cannot find condition in when expression")
      val statements = currentElement.then?.let { thenExpression ->
        (thenExpression as? KtBlockExpression)?.statements
      } ?: error("Cannot find statements")
      val expressionLines = statements.mapNotNull { it.getGeneratedCodeLine(promptToCode) }
      val expressionInWhen = (whenExpression.entries[i]?.expression as? KtBlockExpression)?.statements?.toList()
                             ?: error("Cannot find expression in when expression")
      if (i == 0) {
        newPromptToCode.add(conditionLine.copy(codeLineNumber = lineNumber, generatedCodeLine = WHEN_AND_OPEN_BRACE))
        lineNumber++
      }
      val newCondition = findExpression(psiFile, conditionInWhen)
      newPromptToCode.add(conditionLine.copy(codeLineNumber = lineNumber, generatedCodeLine = newCondition))
      lineNumber++
      expressionLines.forEachIndexed { index, expressionLine ->
        newPromptToCode.add(expressionLine.copy(codeLineNumber = lineNumber, generatedCodeLine = expressionInWhen[index].text))
        lineNumber++
      }
      if (newCondition.last() != CLOSE_BRACE) {
        newPromptToCode.add(conditionLine.copy(codeLineNumber = lineNumber, generatedCodeLine = CLOSE_BRACE.toString()))
        lineNumber += 2
      }
      currentElement = currentElement.`else` as? KtIfExpression ?: break
    }
    Pair(lineNumber, currentElement)
  }

  private fun addElseBranchIfNeeded(
    currentElement: KtIfExpression,
    whenExpression: KtWhenExpression,
    newPromptToCode: MutableList<GeneratedCodeLine>,
    codeLineNumber: Int,
    promptToCode: PromptToCodeResponse
  ): Int = runReadAction {
    if (currentElement.`else` == null) return@runReadAction codeLineNumber
    var lineNumber = codeLineNumber
    val statements = currentElement.`else`.let { thenExpression ->
      (thenExpression as? KtBlockExpression)?.statements
    } ?: error("Cannot find statements")
    val expressionLines = statements.mapNotNull { it.getGeneratedCodeLine(promptToCode) }
    val firstExpressionLine = expressionLines.first()
    val expressionInWhen = (whenExpression.entries.last().expression as? KtBlockExpression)?.statements?.toList()
                           ?: error("Cannot find expression in when expression")
    newPromptToCode.add(firstExpressionLine.copy(codeLineNumber = lineNumber, generatedCodeLine = WHEN_ELSE))
    lineNumber++
    expressionLines.forEachIndexed { index, expressionLine ->
      newPromptToCode.add(expressionLine.copy(codeLineNumber = lineNumber, generatedCodeLine = expressionInWhen[index].text))
      lineNumber++
    }
    newPromptToCode.add(firstExpressionLine.copy(codeLineNumber = lineNumber, generatedCodeLine = CLOSE_BRACE.toString()))
    lineNumber++
    lineNumber
  }

  private fun addEndOfCode(
    newPromptToCode: MutableList<GeneratedCodeLine>,
    codeLineNumber: Int,
    promptToCode: PromptToCodeResponse,
    lastIfPromptLine: Int,
    condition: KtExpression?
  ) = runReadAction {
    var lineNumber = codeLineNumber
    val conditionLine = condition?.getGeneratedCodeLine(promptToCode) ?: error("Cannot find condition")
    newPromptToCode.add(conditionLine.copy(codeLineNumber = lineNumber, generatedCodeLine = CLOSE_BRACE.toString()))
    lineNumber++

    promptToCode.filter { it.promptLineNumber > lastIfPromptLine }.forEach {
      newPromptToCode.add(it.copy(codeLineNumber = lineNumber))
      lineNumber++
    }
  }

  companion object {
    private const val CLOSE_BRACE = '}'
    private const val WHEN_AND_OPEN_BRACE = "when {"
    private const val WHEN_ELSE = "else -> {"
  }
}
