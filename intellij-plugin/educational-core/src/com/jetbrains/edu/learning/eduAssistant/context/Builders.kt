package com.jetbrains.edu.learning.eduAssistant.context

import com.intellij.openapi.application.runReadAction
import com.jetbrains.edu.learning.actions.NextStepHintAction.Companion.isNextStepHintApplicable
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.eduAssistant.AuthorSolutionContext
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

// todo: Enhance by Encapsulating Logic of AuthorSolutionContext
fun createAuthorSolutionContext(task: Task): AuthorSolutionContext? {
  val project = task.project ?: return null
  val language = task.course.languageById ?: return null
  val files = task.taskFiles.values.filterNot { it.isTestFile }
  val functionsToStringMap = runReadAction {
    FunctionsToStrings.create(project, language, files)
  }

  return AuthorSolutionContext(
    functionsToStringMap.mapOfStringSignatures(),
    functionsToStringMap.signatures()
  )
}

fun Course.createAuthorSolutionContext() = allTasks.forEach { it.createAuthorSolutionContext() }

fun Task.createAuthorSolutionContext() {
  if (!isNextStepHintApplicable(this) || authorSolutionContext != null) return
  authorSolutionContext = createAuthorSolutionContext(this)
}
