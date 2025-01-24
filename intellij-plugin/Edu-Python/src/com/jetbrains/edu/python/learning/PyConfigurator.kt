package com.jetbrains.edu.python.learning

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
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

  override fun courseFileAttributes(): AttributesEvaluator = pythonAttributesEvaluator(super.courseFileAttributes())

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
