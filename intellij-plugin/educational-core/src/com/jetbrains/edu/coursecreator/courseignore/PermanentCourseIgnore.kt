package com.jetbrains.edu.coursecreator.courseignore

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.configuration.EduConfigurator

/**
 * Stores rules to ignore additional files during course archive creation, that are always applied independently
 * of what is specified in the `.courseignore` file.
 * These rules are generated from the list of [IgnoringEntry] provided by [EduConfigurator.ignoringEntries]
 */
@Service(Service.Level.PROJECT)
class PermanentCourseIgnore(private val project: Project) {

  val permanentIgnoreRules: CourseIgnoreRules = createPermanentRules()

  private fun createPermanentRules(): CourseIgnoreRules {
    val configurator = project.course?.configurator ?: return CourseIgnoreRules.EMPTY
    return CourseIgnoreRules.fromIgnoringEntries(project, configurator.ignoringEntries())
  }

  companion object {
    fun getInstance(project: Project): PermanentCourseIgnore = project.service()
  }
}