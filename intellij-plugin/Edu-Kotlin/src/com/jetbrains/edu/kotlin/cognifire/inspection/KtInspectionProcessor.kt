package com.jetbrains.edu.kotlin.cognifire.inspection

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import com.jetbrains.edu.cognifire.utils.toGeneratedCode
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import org.jetbrains.kotlin.idea.KotlinLanguage

/**
 * A processor for applying inspections to Kotlin PSI files.
 * The following inspections are supported: LiftReturnOrAssignment, IntroduceWhenSubject, CascadeIf,
 * JoinDeclarationAndAssignment, FoldInitializerAndIfToElvis, ifThenToSafeAccess, IfThenToElvis. TODO(support more inspections)
 */
class KtInspectionProcessor : InspectionProcessor {

  override fun applyInspections(promptToCodeTranslation: PromptToCodeContent, project: Project, functionSignature: String): PromptToCodeContent {
    var promptToCode = promptToCodeTranslation.map { it.copy() }
    val psiFile = getPsiFile(promptToCodeTranslation, functionSignature, project)
    psiFile.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        promptToCode = SupportedInspections.values().fold(promptToCode) { acc, inspection ->
          inspection.applyInspection(project, element, psiFile, acc)
        }
        super.visitElement(element)
      }
    })
    return promptToCode
  }

  private fun getPsiFile(promptToCode: PromptToCodeContent, functionSignature: String, project: Project) = runReadAction {
    PsiFileFactory.getInstance(project).createFileFromText(
      "Main.kt",
      KotlinLanguage.INSTANCE,
      """
        $functionSignature {
            ${promptToCode.toGeneratedCode()}
        }
      """.trimIndent()
    )
  }
}
