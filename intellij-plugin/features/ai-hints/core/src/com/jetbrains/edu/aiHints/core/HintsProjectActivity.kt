package com.jetbrains.edu.aiHints.core

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.aiHints.core.context.AuthorSolutionContext
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.isUnitTestMode

class HintsProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (project.isDisposed|| isUnitTestMode) return

    val course = StudyTaskManager.getInstance(project).course ?: return
    if (course.isStudy && course is EduCourse) {
      AuthorSolutionContext.create(course)
    }
  }
}