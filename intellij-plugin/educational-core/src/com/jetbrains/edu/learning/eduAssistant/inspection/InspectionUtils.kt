package com.jetbrains.edu.learning.eduAssistant.inspection

import com.intellij.codeInsight.daemon.impl.DaemonProgressIndicator
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.learning.eduAssistant.log.Logger

private fun PsiFile.applyLocalInspection(inspection: LocalInspectionTool): List<ProblemDescriptor> {
  val problems = mutableListOf<ProblemDescriptor>()
  val inspectionManager = InspectionManager.getInstance(project)
  ProgressManager.getInstance().executeProcessUnderProgress(
    {
      val hasProblems = problems.addAll(
        runReadAction<List<ProblemDescriptor>> {
          inspection.processFile(this, inspectionManager)
        })
      if (hasProblems) {
        Logger.eduAssistantLogger.info("Inspection ${inspection.id} has ${problems.size} problems")
      }
    },
    DaemonProgressIndicator()
  )
  return problems
}

@Suppress("unused") // Used for ai-assistant-validation module
fun PsiFile.getInspectionsWithIssues(inspections: List<LocalInspectionTool>): List<LocalInspectionTool> = inspections.flatMap { inspection ->
  this.applyLocalInspection(inspection).map { inspection }
}

fun applyInspections(code: String, project: Project, language: Language): String {
  val psiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("file", language, code) }
  val inspections = runReadAction { InspectionProvider.getInspections(language) }
  for (inspection in inspections) {
    psiFile.applyLocalInspection(inspection).forEach { descriptor ->
      descriptor.fixes?.firstOrNull()?.let { quickFix ->
        WriteCommandAction.runWriteCommandAction(project, null, null, {
          quickFix.applyFix(project, descriptor) }, psiFile)
        runReadAction { Logger.eduAssistantLogger.info("Applied ${quickFix.name} quick fix for ${inspection.id} inspection") }
      }
    }
  }
  return runReadAction<String> { psiFile.text }
}
