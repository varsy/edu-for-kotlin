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

private fun PsiFile.applyLocalInspection(inspection: LocalInspectionTool): List<ProblemDescriptor> {
  val problems = mutableListOf<ProblemDescriptor>()
  val inspectionManager = InspectionManager.getInstance(project)
  ProgressManager.getInstance().executeProcessUnderProgress(
    {
      problems.addAll(
        runReadAction<List<ProblemDescriptor>> {
          inspection.processFile(this, inspectionManager)
        })
    },
    DaemonProgressIndicator()
  )
  return problems
}

fun applyInspections(code: String, project: Project, language: Language): String {
  val psiFile = runReadAction { PsiFileFactory.getInstance(project).createFileFromText("file", language, code) }
  val inspections = runReadAction { InspectionProvider.getInspections(language) }
  for (inspection in inspections) {
    psiFile.applyLocalInspection(inspection).forEach { descriptor ->
      descriptor.fixes?.firstOrNull()?.let { quickFix ->
        WriteCommandAction.runWriteCommandAction(project, null, null, {
          quickFix.applyFix(project, descriptor) }, psiFile)
      }
    }
  }
  return runReadAction<String> { psiFile.text }
}
