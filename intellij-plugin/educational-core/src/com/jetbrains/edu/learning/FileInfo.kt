package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG

fun VirtualFile.fileInfo(project: Project): FileInfo? {
  if (project.isDisposed) return null

  if (isDirectory) {
    getSection(project)?.let { return FileInfo.SectionDirectory(it) }
    getLesson(project)?.let { return FileInfo.LessonDirectory(it) }
    getTask(project)?.let { return FileInfo.TaskDirectory(it) }
  }

  val task = getContainingTask(project) ?: return null
  if (shouldIgnore(this, project, task)) return null

  val taskRelativePath = pathRelativeToTask(project)

  return FileInfo.FileInTask(task, taskRelativePath)
}

private fun shouldIgnore(file: VirtualFile, project: Project, task: Task): Boolean {
  val courseDir = project.courseDir
  if (!FileUtil.isAncestor(courseDir.path, file.path, true)) return true
  if (file.isTaskSpecialFile()) return true
  return task.shouldBeEmpty(file.path)
}

/**
 * Must be called for files inside a task directory.
 * Returns, whether this is a `task-info.yaml`, `task-remote-info.yaml` or a task description file.
 */
fun VirtualFile.isTaskSpecialFile(): Boolean {
  return EduUtilsKt.isTaskDescriptionFile(name) || name == TASK_CONFIG || name == REMOTE_TASK_CONFIG
}

sealed class FileInfo {
  data class SectionDirectory(val section: Section) : FileInfo()
  data class LessonDirectory(val lesson: Lesson) : FileInfo()
  data class TaskDirectory(val task: Task) : FileInfo()
  data class FileInTask(val task: Task, val pathInTask: String) : FileInfo()
}
