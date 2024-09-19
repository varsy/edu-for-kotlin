package com.jetbrains.edu.learning.eduAssistant.context.differ

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.courseFormat.eduAssistant.SignatureSource
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignatureResolver
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignaturesProvider
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.createPsiFileForSolution
import com.jetbrains.edu.learning.navigation.NavigationUtils

fun getChangedContent(task: Task, project: Project): String? {
  findChangedFunctions(task, project)
  return task.taskFilesWithChangedFunctions?.keys?.joinToString(separator = System.lineSeparator()) { taskFileName ->
    val virtualFile = task.taskFiles[taskFileName]?.getVirtualFile(project) ?: return@joinToString ""
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return@joinToString ""
    psiFile.filterAllowedModifications(task, project, SignatureSource.VISIBLE_FILE)
  }
}

private fun findChangedFunctions(task: Task, project: Project) {
  if (task.taskFilesWithChangedFunctions != null) return
  val language = task.course.languageById ?: return
  val previousTask = NavigationUtils.previousTask(task) ?: return
  val taskFileNamesToChangedFunctions = mutableMapOf<String, List<String>>()
  val visibleTaskFiles = task.taskFiles.values.filter { it.isVisible }
  for (taskFile in visibleTaskFiles) {
    val previousTaskFile = previousTask.taskFiles[taskFile.name] ?: continue
    val beforePsiFile = previousTaskFile.getSolution().createPsiFileForSolution(project, language)
    val afterPsiFile = taskFile.getSolution().createPsiFileForSolution(project, language)
    val changedFunctions = FilesDiffer.findDifferentMethods(beforePsiFile, afterPsiFile, language)
    if (!changedFunctions.isNullOrEmpty()) {
      taskFileNamesToChangedFunctions[taskFile.name] = changedFunctions
    }
  }
  if (taskFileNamesToChangedFunctions.isNotEmpty()) {
    task.taskFilesWithChangedFunctions = taskFileNamesToChangedFunctions
  }
}

/**
 * Filters the allowed changed functions in the psi file at the current stage:
 * which are either contained in task.changedFunctions (updated functions in the authoring solution in comparison with the previous step)
 * or not contained in the author's solution.
 */
fun PsiFile.filterAllowedModifications(task: Task, project: Project, signatureSource: SignatureSource): String {
  findChangedFunctions(task, project)
  val language = task.course.languageById ?: return ""
  return runReadAction {
    FunctionSignaturesProvider.getFunctionSignatures(this, signatureSource, language).filter { functionSignature ->
      task.taskFilesWithChangedFunctions?.values?.flatten()?.contains(functionSignature.name) == true ||
      task.authorSolutionContext?.functionSignatures?.contains(functionSignature) == false
    }.joinToString(separator = System.lineSeparator()) {
      FunctionSignatureResolver.getFunctionBySignature(this, it.name, language)?.text ?: ""
    }
  }
}
