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
import com.jetbrains.edu.learning.actions.EduActionUtils.runUndoableAction
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.isTaskSpecialFile
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

    val affectedFiles = mutableListOf<VirtualFile>()
    val states = mutableListOf<State>()
    val tasks = mutableSetOf<Task>()

    fun collect(files: List<VirtualFile>) {
      for (file in files) {
        val task = file.getContainingTask(project) ?: return
        if (file.isTaskSpecialFile()) continue
        if (file.isDirectory) {
          collect(VfsUtil.collectChildrenRecursively(file).filter { !it.isDirectory })
        }
        else {
          affectedFiles += file
        }

        states += createStateForFile(project, task, file) ?: continue
        tasks += task
      }
    }

    collect(virtualFiles)

    val action = ChangeFilesPropertyUndoableAction(project, states, tasks, affectedFiles)
    runUndoableAction(project, name.get(), action)
  }

  protected open fun isAvailableForFile(project: Project, file: VirtualFile): Boolean {
    val task = file.getContainingTask(project) ?: return false
    return if (file.isDirectory) {
      // Recursive check is too expensive for `update` method
      // so we allow this action for directories
      true
    }
    else {
      isAvailableForSingleFile(project, task, file)
    }
  }

  protected abstract fun isAvailableForSingleFile(project: Project, task: Task, file: VirtualFile): Boolean
  protected abstract fun createStateForFile(project: Project, task: Task, file: VirtualFile): State?
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
