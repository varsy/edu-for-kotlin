package com.jetbrains.edu.python.learning

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
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

  override fun courseFileAttributes(): AttributesEvaluator = pythonAttributesEvaluator(super.courseFileAttributes())

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
