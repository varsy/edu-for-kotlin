package com.jetbrains.edu.coursecreator.framework.diff

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

fun applyChangesWithMergeDialog(
  project: Project,
  currentTask: Task,
  targetTask: Task,
  conflictFiles: List<String>,
  leftState: FLTaskState,
  baseState: FLTaskState,
  rightState: FLTaskState,
  initialBaseState: FLTaskState = baseState
): FLTaskState? {
  val mergeProvider = FLMergeProvider(project, targetTask, leftState, baseState, rightState, initialBaseState)
  val mergeDialogCustomizer = FLMergeDialogCustomizer(project, currentTask.name, targetTask.name)
  val tmpConflictFiles = conflictFiles.map { name ->
    val fileType = findConflictFileType(project, name, currentTask, targetTask) ?: error("Couldn't find file corresponding for $name")
    val fileContent = baseState[name] ?: error("Conflict file content was not added to baseState during conflict resolution")
    LightVirtualFile(name, fileType, fileContent)
  }
  val isOk = showMultipleFileMergeDialog(project, tmpConflictFiles, mergeProvider, mergeDialogCustomizer, currentTask.name, targetTask.name)

  // merge dialog was canceled
  if (!isOk) {
    return null
  }

  val finalState = baseState.toMutableMap()

  for (file in tmpConflictFiles) {
    // file was deleted
    if (!file.exists()) {
      finalState.remove(file.name)
    }

    val document = FileDocumentManager.getInstance().getDocument(file)

    finalState[file.name] = document?.text ?: error("There is no document for ${file.name}")
  }

  return finalState
}

private var MOCK: FLMultipleFileMergeUI? = null

@TestOnly
fun withFLMultipleFileMergeUI(mockUi: FLMultipleFileMergeUI, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  }
  finally {
    MOCK = null
  }
}

fun showMultipleFileMergeDialog(
  project: Project,
  files: List<VirtualFile>,
  mergeProvider: FLMergeProvider,
  mergeCustomizer: FLMergeDialogCustomizer,
  currentTaskName: String,
  targetTaskName: String
): Boolean {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("You should set mock UI via `withMockSelectTaskUi`")
  }
  else {
    FLMultipleFileMergeUIImpl()
  }
  return ui.show(project, files, mergeProvider, mergeCustomizer, currentTaskName, targetTaskName)
}

fun resolveConflicts(
  project: Project,
  currentState: FLTaskState,
  baseState: FLTaskState,
  targetState: FLTaskState
): FLConflictResolveStrategy.StateWithResolvedChanges {
  return with(DiffConflictResolveStrategy(project)) {
    try {
      resolveConflicts(currentState, baseState, targetState)
    } finally {
      Disposer.dispose(this)
    }
  }
}

private fun findConflictFileType(project: Project, fileName: String, currentTask: Task, targetTask: Task): FileType? {
  // conflict file must exist as virtual file on disk either in the current task or target task
  // otherwise there cannot be a conflict
  return findFileType(project, fileName, currentTask) ?: findFileType(project, fileName, targetTask)
}

private fun findFileType(project: Project, fileName: String, task: Task): FileType? {
  val taskFile = task.taskFiles[fileName] ?: return null
  return taskFile.getVirtualFile(project)?.fileType
}