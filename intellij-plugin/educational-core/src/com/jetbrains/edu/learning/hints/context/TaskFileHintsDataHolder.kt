package com.jetbrains.edu.learning.hints.context

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.rd.util.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class TaskFileHintsDataHolder {
  private val data = ConcurrentHashMap<TaskFile, TaskFileHintData>()

  private fun getOrCreate(taskFile: TaskFile): TaskFileHintData = data.getOrPut(taskFile) { TaskFileHintData() }

  data class TaskFileHintData(
    val functionSignatures: FunctionSignatures = FunctionSignatures(),
    var snapshotFileHash: Int? = null,
    val usedStrings: UsedStrings = UsedStrings(),
  )

  data class FunctionSignatures(
    var value: List<FunctionSignature>? = null,
    var snapshotHash: Int? = null,
  )

  data class UsedStrings(
    var value: List<String>? = null,
    var snapshotHash: Int? = null
  )

  companion object {
    fun getInstance(project: Project): TaskFileHintsDataHolder = project.service()

    val TaskFile.hintData: TaskFileHintData?
      get() {
        val project = task.project ?: return null
        return getInstance(project).getOrCreate(this)
      }
  }
}