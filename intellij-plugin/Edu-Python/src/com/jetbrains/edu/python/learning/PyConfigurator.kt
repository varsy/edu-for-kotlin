package com.jetbrains.edu.python.learning

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.ArchiveFileInfo
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.IncludeType
import com.jetbrains.edu.learning.configuration.buildArchiveFileInfo
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.checker.PyTaskCheckerProvider
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import javax.swing.Icon

open class PyConfigurator : EduConfigurator<PyProjectSettings> {
  override val courseBuilder: EduCourseBuilder<PyProjectSettings>
    get() = PyCourseBuilder()

  override fun getMockFileName(course: Course, text: String): String = TASK_PY

  override val testFileName: String
    get() = TESTS_PY

  override fun archiveFileInfo(holder: CourseInfoHolder<out Course?>, file: VirtualFile): ArchiveFileInfo =
    buildArchiveFileInfo(holder, file) {
      when {
        excludeFromArchive(file) -> {
          description("Python compiled files or libraries")
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        else -> info(super.archiveFileInfo(holder, file))
      }
    }

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PyTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Python

  override val isCourseCreatorEnabled: Boolean
    get() = false

  override val defaultPlaceholderText: String
    get() = "# TODO"

  companion object {
    const val TESTS_PY = "tests.py"
    const val TASK_PY = "task.py"
    const val MAIN_PY = "main.py"
  }
}
