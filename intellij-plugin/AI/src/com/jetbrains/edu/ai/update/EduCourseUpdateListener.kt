package com.jetbrains.edu.ai.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.terms.TermsLoader
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.learning.CourseUpdateListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse

class EduCourseUpdateListener : CourseUpdateListener {
  override fun courseUpdated(project: Project, course: Course) {
    val eduCourse = project.course as? EduCourse ?: return
    TranslationLoader.getInstance(project).updateTranslationWhenCourseUpdate(eduCourse)
    TermsLoader.getInstance(project).updateTermsWhenCourseUpdate(eduCourse)
  }
}