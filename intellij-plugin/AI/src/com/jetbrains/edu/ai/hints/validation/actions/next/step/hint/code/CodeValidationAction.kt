package com.jetbrains.edu.ai.hints.validation.actions.next.step.hint.code

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.ai.hints.validation.actions.ValidationAction
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.inspection.InspectionProvider
import com.jetbrains.edu.learning.eduAssistant.inspection.getInspectionsWithIssues
import com.jetbrains.edu.learning.framework.FrameworkLessonManager

abstract class CodeValidationAction<T> : ValidationAction<T>() {
  private fun checkTaskFile(taskFile: TaskFile) = (taskFile.isBinary == false) && taskFile.isVisible

  protected fun getCodeFromTaskFiles(task: EduTask, lesson: Lesson): List<String> {
    val project = task.project ?: error("Cannot get project")
    if (lesson is FrameworkLesson) {
      val state: Map<String, String> = FrameworkLessonManager.getInstance(project).getTaskState(lesson, task)
      return state.filterKeys { task.taskFiles[it] != null && checkTaskFile(task.taskFiles[it]!!) }.values.toList()
    }
    return task.taskFiles.values.filter { checkTaskFile(it) }.mapNotNull { it.getText(project) }
  }

  protected fun runInspections(project: Project, language: Language, code: String) = PsiFileFactory.getInstance(project).createFileFromText(
    "file", language, code
  ).getInspectionsWithIssues(InspectionProvider.getInspections(language)).map { it.id }
}
