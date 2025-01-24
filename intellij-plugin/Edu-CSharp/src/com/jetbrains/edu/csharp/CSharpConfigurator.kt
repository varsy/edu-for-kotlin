package com.jetbrains.edu.csharp

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.csharp.checker.CSharpTaskCheckerProvider
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.rider.ideaInterop.fileTypes.sln.SolutionFileType
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

// BACKCOMPAT: 2024.2
private val BUILD_242_21829 = BuildNumber.fromString("242.21829")!!

class CSharpConfigurator : EduConfigurator<CSharpProjectSettings> {
  override val courseBuilder: EduCourseBuilder<CSharpProjectSettings>
    get() = CSharpCourseBuilder()

  override val testFileName: String
    get() = TEST_CS

  override val sourceDir: String
    get() = SRC_DIRECTORY

  override val testDirs: List<String>
    get() = listOf(TEST_DIRECTORY)

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CSharpTaskCheckerProvider()

  override fun courseFileAttributes() = AttributesEvaluator(super.courseFileAttributes()) {
    extension(SolutionFileType.defaultExtension) {
      excludeFromArchive()
    }

    path(BIN_DIRECTORY, OBJ_DIRECTORY) {
      excludeFromArchive()
    }
  }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  override val isEnabled: Boolean
    get() = isFeatureEnabled(EduExperimentalFeatures.CSHARP_COURSES) &&
            ApplicationInfo.getInstance().build >= BUILD_242_21829

  override fun getMockFileName(course: Course, text: String): String = TASK_CS
  override val logo: Icon
    get() = EducationalCoreIcons.Language.CSharp

  companion object {
    @NonNls
    const val TASK_CS = "Task.cs"

    @NonNls
    const val TEST_CS = "Test.cs"

    const val SRC_DIRECTORY = "src"
    const val TEST_DIRECTORY = "test"

    const val BIN_DIRECTORY = "bin"
    const val OBJ_DIRECTORY = "obj"
  }
}