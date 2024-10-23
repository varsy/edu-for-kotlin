package com.jetbrains.edu.learning.eduAssistant.context

import com.intellij.openapi.application.runReadAction
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.learning.actions.EduActionUtils.isGetHintApplicable
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.*
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
    fun create(course: Course) = runReadAction {
      course.allTasks.forEach { task ->
        if (isGetHintApplicable(task) && task.authorSolutionContext == null) {
          task.authorSolutionContext = create(task)
        }
      }
    }
  }
}
