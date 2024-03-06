@file:JvmName("TaskFileExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.WolfTheProblemSolver
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFile.Companion.LOG
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer


fun TaskFile.getDocument(project: Project): Document? {
  val virtualFile = getVirtualFile(project) ?: return null
  return runReadAction { FileDocumentManager.getInstance().getDocument(virtualFile) }
}

fun TaskFile.getVirtualFile(project: Project): VirtualFile? {
  val taskDir = task.getDir(project.courseDir) ?: return null
  return this.findTaskFileInDir(taskDir)
}

fun TaskFile.findTaskFileInDir(taskDir: VirtualFile): VirtualFile? {
  return taskDir.findFileByRelativePath(name)
}

fun TaskFile.course() = task.lesson.course

fun TaskFile.getText(project: Project): String? = getDocument(project)?.text

val TaskFile.isTestFile: Boolean
  get() {
    val configurator = task.course.configurator ?: return false
    return configurator.isTestFile(task, name)
  }


fun TaskFile.revert(project: Project) {
  if (!resetDocument(project)) {
    return
  }

  answerPlaceholders.forEach { answerPlaceholder ->
    answerPlaceholder.reset(true)
  }

  val virtualFile = getVirtualFile(project)
  if (virtualFile != null) {
    WolfTheProblemSolver.getInstance(project).clearProblems(virtualFile)
  }
  if (errorHighlightLevel == EduFileErrorHighlightLevel.ALL_PROBLEMS) {
    errorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
  }
  YamlFormatSynchronizer.saveItem(task)
}

fun TaskFile.getSolution(): String {
  if (this.task.lesson is FrameworkLesson) {
    return this.contents.textualRepresentation
  } else {
    val fullAnswer = StringBuilder(this.contents.textualRepresentation)
    this.answerPlaceholders.sortedBy { it.offset }.reversed().forEach { placeholder ->
      placeholder.possibleAnswer.let { answer ->
        fullAnswer.replace(
          placeholder.initialState.offset,
          placeholder.initialState.offset + placeholder.initialState.length, answer
        )
      }
    }
    return fullAnswer.toString()
  }
}

/**
 * @return true if document related to task file has been reset, otherwise - false
 */
private fun TaskFile.resetDocument(project: Project): Boolean {
  val document = getDocument(project)
  // Note, nullable document is valid situation in case of binary files.
  if (document == null) {
    LOG.warning("Failed to find document for task file $name")
    return false
  }

  isTrackChanges = false
  document.setText(contents.textualRepresentation)
  isTrackChanges = true
  return true
}

fun TaskFile.shouldBePropagated(): Boolean = isEditable && isVisible

/**
 * Updates the content of the [TaskFile] with the specified [newContent].
 * Only for testing or validation.
 */
fun TaskFile.updateContent(project: Project, newContent: String) {
  val document = getDocument(project) ?: error("Document was not found")
  runWriteAction {
    document.setText(newContent)
    FileDocumentManager.getInstance().saveDocument(document)
    PsiDocumentManager.getInstance(project).commitDocument(document)
  }
}
