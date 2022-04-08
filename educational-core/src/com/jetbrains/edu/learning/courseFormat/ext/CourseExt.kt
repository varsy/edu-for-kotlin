@file:JvmName("CourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse

val Course.configurator: EduConfigurator<*>? get() {
  val language = languageById ?: return null
  return EduConfiguratorManager.findConfigurator(itemType, environment, language)
}

val Course.compatibilityProvider: CourseCompatibilityProvider?
  get() {
    return CourseCompatibilityProviderEP.find(languageID, environment)
  }

val Course.sourceDir: String? get() = configurator?.sourceDir
val Course.testDirs: List<String> get() = configurator?.testDirs.orEmpty()

val Course.project: Project? get() {
  for (project in ProjectManager.getInstance().openProjects) {
    if (this == StudyTaskManager.getInstance(project).course) {
      return project
    }
  }
  return null
}

val Course.hasSections: Boolean get() = sections.isNotEmpty()

val Course.hasTopLevelLessons: Boolean get() = lessons.isNotEmpty()

val Course.allTasks: List<Task> get() {
  val allTasks = mutableListOf<Task>()
  course.visitTasks { allTasks += it }
  return allTasks
}

val Course.languageDisplayName: String get() = languageById?.displayName ?: languageID

val Course.technologyName: String?
  get() = compatibilityProvider?.technologyName ?: languageById?.displayName

val Course.supportedTechnologies: List<String>
  get() {
    return when (this) {
      is JetBrainsAcademyCourse -> this.supportedLanguages
      else -> if (technologyName != null) listOf(technologyName!!) else emptyList()
    }
  }