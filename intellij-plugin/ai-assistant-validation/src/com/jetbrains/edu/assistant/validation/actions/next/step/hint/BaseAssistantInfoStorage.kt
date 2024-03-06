package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.eduState
import com.jetbrains.educational.ml.hints.assistant.AiHintsAssistant

data class BaseAssistantInfoStorage(private val task: EduTask) {
  val taskProcessor = TaskProcessorImpl(task)
  val assistant get() = AiHintsAssistant.getAssistant(taskProcessor)
  val language = task.course.languageById ?: error("Language could not be determined")
  val project get() = task.project ?: error("Cannot get project")

  val eduState get() = project.eduState ?: error("Cannot get eduState for project ${project.name}")
}
