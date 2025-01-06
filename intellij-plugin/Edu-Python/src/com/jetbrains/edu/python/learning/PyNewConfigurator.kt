package com.jetbrains.edu.python.learning

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.ArchiveFileInfo
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.IncludeType
import com.jetbrains.edu.learning.configuration.buildArchiveFileInfo
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.TASK_PY
import com.jetbrains.edu.python.learning.checker.PyNewTaskCheckerProvider
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import javax.swing.Icon

class PyNewConfigurator : EduConfigurator<PyProjectSettings> {
  override val courseBuilder: PyNewCourseBuilder
    get() = PyNewCourseBuilder()

  override val testFileName: String
    get() = TEST_FILE_NAME

  override fun getMockFileName(course: Course, text: String): String = TASK_PY

  override val testDirs: List<String>
    get() = listOf(TEST_FOLDER)

  override fun archiveFileInfo(holder: CourseInfoHolder<out Course?>, file: VirtualFile): ArchiveFileInfo =
    buildArchiveFileInfo(holder, file) {
      when {
        excludeFromArchive(file) -> {
          description("Python compiled files or libraries}")
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        else -> info(super.archiveFileInfo(holder, file))
      }
    }

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PyNewTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Python

  override val defaultPlaceholderText: String
    get() = "# TODO"

  companion object {
    const val TEST_FILE_NAME = "test_task.py"
    const val TEST_FOLDER = "tests"
  }
}
