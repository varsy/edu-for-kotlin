package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.undo.BasicUndoableAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtilsKt.canNotBeTaskFile
import com.jetbrains.edu.learning.actions.EduActionUtils.runUndoableAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem
import java.util.function.Supplier

abstract class CCChangeFilePropertyActionBase(
  private val name: Supplier<@ActionText String>
) : DumbAwareAction(name) {

  override fun update(e: AnActionEvent) {
    val project = e.project
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    if (project == null || !CCUtils.isCourseCreator(project)) return
    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext).orEmpty()

    presentation.isEnabledAndVisible = when (virtualFiles.size) {
      0 -> false
      1 -> isAvailableForFile(project, virtualFiles.single())
      else -> virtualFiles.all { isAvailableForFile(project, it) }
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val virtualFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(e.dataContext)?.toList() ?: return

    val affectedFiles = collectAffectedFiles(project, virtualFiles)
    val states = mutableListOf<State>()
    val tasks = mutableSetOf<Task>()
    for (file in affectedFiles) {
      val task = file.getContainingTask(project) ?: continue
      states += createStateForFile(project, task, file) ?: continue
      tasks += task
    }

    val action = ChangeFilesPropertyUndoableAction(project, states, tasks, affectedFiles)
    runUndoableAction(project, name.get(), action)
  }

  protected open fun isAvailableForFile(project: Project, file: VirtualFile): Boolean {
    val task = file.getContainingTask(project) ?: return false
    return if (file.isDirectory) {
      isAvailableForDirectory(project, task, file)
    }
    else {
      isAvailableForSingleFile(project, task, file)
    }
  }

  // Recursive check is too expensive for `update` method,
  // so we allow this action for directories by default
  protected open fun isAvailableForDirectory(project: Project, task: Task, directory: VirtualFile): Boolean = true

  protected abstract fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean
  protected abstract fun createStateForFile(project: Project, task: Task, file: VirtualFile): State?

  protected open fun collectAffectedFiles(project: Project, files: List<VirtualFile>): List<VirtualFile> {
    val affectedFiles = mutableListOf<VirtualFile>()
    for (file in files) {
      if (canNotBeTaskFile(file) || file.getContainingTask(project) == null) continue
      if (file.isDirectory) {
        affectedFiles += collectAffectedFiles(project, VfsUtil.collectChildrenRecursively(file).filter { !it.isDirectory })
      }
      else {
        affectedFiles += file
      }
    }
    return affectedFiles
  }
}

private class ChangeFilesPropertyUndoableAction(
  private val project: Project,
  private val states: List<State>,
  private val affectedTasks: Collection<Task>,
  files: List<VirtualFile>
) : BasicUndoableAction(*files.toTypedArray()) {

  override fun redo() = doAction { it.changeState(project) }
  override fun undo() = doAction { it.restoreState(project) }

  private inline fun doAction(changeState: (State) -> Unit) {
    states.forEach(changeState)
    ProjectView.getInstance(project).refresh()
    affectedTasks.forEach { saveItem(it) }
  }

  override fun isGlobal(): Boolean = true
}

interface State {
  fun changeState(project: Project)
  fun restoreState(project: Project)
}
