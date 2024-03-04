package com.jetbrains.edu.python.learning

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.coursecreator.courseignore.IgnoringEntry
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.compatibility.isDataSpellSupported
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

  override fun ignoringEntries(): List<IgnoringEntry> = super.ignoringEntries() + pyIgnoringEntries()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PyTaskCheckerProvider()

  override val logo: Icon
    get() = pythonIcon

  override val isCourseCreatorEnabled: Boolean
    get() = false

  override val defaultPlaceholderText: String
    get() = "# TODO"

  // BACKCOMPAT: 2023.2
  override val isEnabled: Boolean
    get() = if (PlatformUtils.isDataSpell()) isDataSpellSupported else super.isEnabled

  companion object {
    const val TESTS_PY = "tests.py"
    const val TASK_PY = "task.py"
    const val MAIN_PY = "main.py"
  }
}
