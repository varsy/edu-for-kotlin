package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse.GeneratedCodeLine
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import org.jetbrains.kotlin.idea.intentions.JoinDeclarationAndAssignmentIntention
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.scripting.definitions.runReadAction

class JoinDeclarationAndAssignmentInspectionProcessing(private val project: Project, private val element: KtProperty) :
  InspectionProcessing {
  private val intention = JoinDeclarationAndAssignmentIntention()

  override fun isApplicable(): Boolean = runReadAction {
    if (!element.isValid) return@runReadAction false
    intention.applicabilityRange(element) != null
  }

  override fun apply() {
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      intention.applyTo(element, null)
    })
  }

  override fun applyInspection(promptToCode: PromptToCodeContent, psiFile: PsiFile): PromptToCodeContent {
    if (!isApplicable()) return promptToCode
    val property = element.copy() as KtProperty
    val definition = promptToCode.find { it.generatedCodeLine == property.text } ?: return promptToCode
    val binaryExpression = PsiTreeUtil.collectElementsOfType(psiFile, KtBinaryExpression::class.java).toList()
      .firstOrNull { (it.left as? KtNameReferenceExpression)?.getReferencedName() == property.name }
    val assignment = binaryExpression?.let {
      promptToCode.find { it.generatedCodeLine == binaryExpression.text }
    } ?: return promptToCode

    apply()

    val newPromptToCode = mutableListOf<GeneratedCodeLine>()
    promptToCode.forEach {
      if (it.promptLineNumber < definition.promptLineNumber) {
        newPromptToCode.add(it.copy())
      } else if (it.promptLineNumber == definition.promptLineNumber) {
        newPromptToCode.add(it.copy(codeLineNumber = assignment.codeLineNumber - 1, generatedCodeLine = element.text))
      } else if (it.promptLineNumber == assignment.promptLineNumber && it.codeLineNumber == assignment.codeLineNumber) {
        newPromptToCode.add(it.copy(codeLineNumber = assignment.codeLineNumber - 1, generatedCodeLine = element.text))
      } else {
        newPromptToCode.add(it.copy(codeLineNumber = it.codeLineNumber - 1))
      }
    }
    return newPromptToCode
  }
}
