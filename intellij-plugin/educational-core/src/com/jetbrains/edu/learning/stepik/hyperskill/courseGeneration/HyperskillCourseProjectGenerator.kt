package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.newproject.EduProjectSettings

open class HyperskillCourseProjectGenerator<T : EduProjectSettings>(
  private val base: CourseProjectGenerator<T>,
  builder: HyperskillCourseBuilder<T>,
  course: HyperskillCourse
) : CourseProjectGenerator<T>(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: T, onConfigurationFinished: () -> Unit) =
    base.afterProjectGenerated(project, projectSettings, onConfigurationFinished)

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> =
    base.autoCreatedAdditionalFiles(holder)

  override suspend fun createCourseStructure(holder: CourseInfoHolder<Course>, initialLessonProducer: () -> Lesson) =
    base.createCourseStructure(holder, initialLessonProducer)
}
