package com.jetbrains.edu.cpp

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.cpp.checker.CppCatchTaskCheckerProvider
import com.jetbrains.edu.cpp.checker.CppGTaskCheckerProvider
import com.jetbrains.edu.cpp.checker.CppTaskCheckerProvider
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.ArchiveFileInfo
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.IncludeType
import com.jetbrains.edu.learning.configuration.buildArchiveFileInfo
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

  override fun archiveFileInfo(holder: CourseInfoHolder<out Course?>, file: VirtualFile): ArchiveFileInfo =
    buildArchiveFileInfo(holder, file) {
      when {
        regex("^cmake-build-") -> {
          description("build directory")
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        regex("^$TEST_FRAMEWORKS_BASE_DIR_VALUE") -> {
          description("google test directory")
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        else -> info(super.archiveFileInfo(holder, file))
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