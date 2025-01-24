package com.jetbrains.edu.cpp

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.cpp.checker.CppCatchTaskCheckerProvider
import com.jetbrains.edu.cpp.checker.CppGTaskCheckerProvider
import com.jetbrains.edu.cpp.checker.CppTaskCheckerProvider
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import javax.swing.Icon

class CppGTestConfigurator : CppConfigurator() {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppGTestCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CppGTaskCheckerProvider()
}

class CppCatchConfigurator : CppConfigurator() {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppCatchCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CppCatchTaskCheckerProvider()
}

open class CppConfigurator : EduConfigurator<CppProjectSettings> {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CppTaskCheckerProvider()

  override val testFileName: String
    get() = TEST_CPP

  override fun getMockFileName(course: Course, text: String): String = MAIN_CPP

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val mockTemplate: String
    get() = getInternalTemplateText(MAIN_CPP)

  override val isCourseCreatorEnabled: Boolean
    get() = true

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Cpp

  override fun courseFileAttributes(): AttributesEvaluator = AttributesEvaluator(super.courseFileAttributes()) {
    // we could use it how indicator because CLion generate build dirs with names `cmake-build-*`
    // @see com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace.getProfileGenerationDirNames
    dir("^cmake-build-".toRegex(), TEST_FRAMEWORKS_BASE_DIR_VALUE) {
      excludeFromArchive()
    }
  }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val MAIN_CPP = "main.cpp"
    const val TASK_CPP = "task.cpp"
    const val TEST_CPP = "test.cpp"
  }
}