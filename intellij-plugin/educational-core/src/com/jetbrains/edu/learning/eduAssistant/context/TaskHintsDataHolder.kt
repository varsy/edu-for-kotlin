package com.jetbrains.edu.learning.eduAssistant.context

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task

@Service(Service.Level.PROJECT)
class TaskHintsDataHolder {
  private val data = mutableMapOf<Task, TaskHintData>()

  private fun getOrCreate(task: Task): TaskHintData = data.getOrPut(task) { TaskHintData() }

  data class TaskHintData(
    var authorSolutionContext: AuthorSolutionContext? = null,
    var taskFilesWithChangedFunctions: Map<String, List<String>>? = null
  )

  companion object {
    fun getInstance(project: Project): TaskHintsDataHolder = project.service()

    val Task.hintData: TaskHintData?
      get() {
        val project = project ?: return null
        return getInstance(project).getOrCreate(this)
      }
  }
}