package com.jetbrains.edu.learning.eduAssistant.context.function.signatures

import com.intellij.lang.Language
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignature
import com.jetbrains.edu.learning.courseFormat.eduAssistant.SignatureSource
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task


fun isFileUnchanged(file: TaskFile, project: Project): Boolean {
  val document = file.getDocument(project) ?: return false
  val currentHash = document.text.hashCode()
  val isUnchanged = file.snapshotFileHash == currentHash
  if (!isUnchanged) file.snapshotFileHash = currentHash
  return isUnchanged
}

fun getFunctionSignaturesIfFileUnchanged(file: TaskFile, project: Project) =
  if (isFileUnchanged(file, project) && file.functionSignaturesSnapshotHash == file.snapshotFileHash)
    file.functionSignatures
  else null
    .also { file.functionSignaturesSnapshotHash = file.snapshotFileHash }

fun String.createPsiFileForSolution(project: Project, language: Language): PsiFile = PsiFileFactory.getInstance(project).createFileFromText(
  "solution", language, this
)

fun getFunctionSignatures(task: Task, file: TaskFile, project: Project): List<FunctionSignature> {
  val language = task.course.languageById ?: return emptyList()
  val virtualFile = file.getVirtualFile(project) ?: return emptyList()
  val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return emptyList()
  val functionSignatures = FunctionSignaturesProvider.getFunctionSignatures(
    psiFile, if (file.isVisible) SignatureSource.VISIBLE_FILE else SignatureSource.HIDDEN_FILE, language
  )
  return functionSignatures
}

fun getFunctionSignaturesFromGeneratedCode(code: String, project: Project, language: Language) = runReadAction {
  val psiFile = code.createPsiFileForSolution(project, language)
  FunctionSignaturesProvider.getFunctionSignatures(psiFile, SignatureSource.GENERATED_SOLUTION, language)
}
