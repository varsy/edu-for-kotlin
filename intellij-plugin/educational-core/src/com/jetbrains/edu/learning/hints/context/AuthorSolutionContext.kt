package com.jetbrains.edu.learning.hints.context

import com.intellij.openapi.application.readAction
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task

data class AuthorSolutionContext(
  val functionsToStringMap: Map<String, List<String>> = emptyMap(),
  val functionSignatures: Set<FunctionSignature> = emptySet()
) {
  companion object {
    @JvmStatic
    @RequiresReadLock
    fun create(task: Task): AuthorSolutionContext? {
      val project = task.project ?: return null
      val language = task.course.languageById ?: return null
      val files = task.taskFiles.values.filterNot { it.isTestFile }
      val functionsToStringMap = FunctionsToStrings.create(project, language, files)
      return AuthorSolutionContext(
        functionsToStringMap.mapOfStringSignatures(),
        functionsToStringMap.signatures()
      )
    }

    @JvmStatic
    suspend fun create(course: Course) = readAction {
      val tasksToInitialize = course.allTasks.filter { it is EduTask && it.authorSolutionContext == null }
      for (task in tasksToInitialize) {
        task.authorSolutionContext = create(task)
      }
    }
  }
}
