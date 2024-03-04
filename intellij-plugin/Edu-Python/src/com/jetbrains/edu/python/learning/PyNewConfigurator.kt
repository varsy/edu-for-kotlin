package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.coursecreator.courseignore.IgnoringEntry
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.compatibility.isDataSpellSupported
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

  override fun ignoringEntries(): List<IgnoringEntry> = super.ignoringEntries() + pyIgnoringEntries()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PyNewTaskCheckerProvider()

  override val logo: Icon
    get() = pythonIcon

  override val defaultPlaceholderText: String
    get() = "# TODO"

  // BACKCOMPAT: 2023.2
  override val isEnabled: Boolean
    get() = if (PlatformUtils.isDataSpell()) isDataSpellSupported else super.isEnabled

  companion object {
    const val TEST_FILE_NAME = "test_task.py"
    const val TEST_FOLDER = "tests"
  }
}
