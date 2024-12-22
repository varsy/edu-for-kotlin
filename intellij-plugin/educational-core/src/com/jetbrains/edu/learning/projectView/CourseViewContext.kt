package com.jetbrains.edu.learning.projectView

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator

data class CourseViewContext(
  private val courseDir: VirtualFile,
  val course: Course,
  val configurator: EduConfigurator<*>
) {

  fun containsAdditionalFile(file: VirtualFile): Boolean {
    val relativePath = FileUtil.getRelativePath(
      courseDir.path,
      file.path,
      VFS_SEPARATOR_CHAR
    )
    return course.additionalFiles.find { it.name == relativePath } != null
  }

  companion object {
    fun create(project: Project, course: Course): CourseViewContext? {
      val configurator = course.configurator ?: return null

      return CourseViewContext(
        project.courseDir,
        course,
        configurator
      )
    }
  }
}
