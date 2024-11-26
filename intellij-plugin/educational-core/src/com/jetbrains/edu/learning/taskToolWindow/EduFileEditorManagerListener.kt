package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class EduFileEditorManagerListener(private val project: Project) : FileEditorManagerListener, FileEditorManagerListener.Before {

  override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
    val taskFile = file.getTaskFile(project) ?: return
    showTaskDescriptionToolWindow(project, taskFile, true)
  }

  override fun selectionChanged(event: FileEditorManagerEvent) {
    val file = event.newFile
    val task = file?.getContainingTask(project) ?: return
    TaskToolWindowView.getInstance(project).currentTask = task
  }

  override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
    if (project.isEduProject() && FileEditorManager.getInstance(project).openFiles.isEmpty()) {
      TaskToolWindowView.getInstance(project).currentTask = null
    }
  }

  private fun showTaskDescriptionToolWindow(project: Project, taskFile: TaskFile, retry: Boolean) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    val studyToolWindow = toolWindowManager.getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW)
    if (studyToolWindow == null) {
      if (retry) {
        toolWindowManager.invokeLater { showTaskDescriptionToolWindow(project, taskFile, false) }
      }
      else {
        LOG.warn(String.format("Failed to get toolwindow with `%s` id", TaskToolWindowFactory.STUDY_TOOL_WINDOW))
      }
      return
    }
    if (taskFile.task != TaskToolWindowView.getInstance(project).currentTask) {
      EduUtilsKt.updateToolWindows(project)
      studyToolWindow.show(null)
    }
  }

  companion object {
    private val LOG = logger<EduFileEditorManagerListener>()
  }
}