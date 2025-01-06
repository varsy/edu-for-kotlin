package com.jetbrains.edu.php

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.ArchiveFileInfo
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.IncludeType
import com.jetbrains.edu.learning.configuration.buildArchiveFileInfo
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.php.composer.ComposerUtils
import javax.swing.Icon

class PhpConfigurator : EduConfigurator<PhpProjectSettings> {

  override val courseBuilder: EduCourseBuilder<PhpProjectSettings>
    get() = PhpCourseBuilder()

  override val testFileName: String
    get() = TEST_PHP

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PhpTaskCheckerProvider()

  override val logo: Icon
    // the default icon from plugin looks ugly, so we use ours
    get() = EducationalCoreIcons.Language.Php

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val sourceDir: String
    get() = EduNames.SRC

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("com.jetbrains.php"))

  override fun getMockFileName(course: Course, text: String): String = TASK_PHP

  override fun isTestFile(task: Task, path: String): Boolean = super.isTestFile(task, path) || path == testFileName

  override fun archiveFileInfo(holder: CourseInfoHolder<out Course?>, file: VirtualFile): ArchiveFileInfo =
    buildArchiveFileInfo(holder, file) {
      when {
        regex("(^|/)${ComposerUtils.VENDOR_DIR_DEFAULT_NAME}(/|$)") -> {
          description("Vendor directory")
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        nameRegex("^${ComposerUtils.COMPOSER_PHAR_NAME}$") -> {
          description("Composer phar")
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        else -> info(super.archiveFileInfo(holder, file))
      }
    }

  companion object {
    const val MAIN_PHP = "main.php"
    const val TASK_PHP = "task.php"
    const val TEST_PHP = "test.php"
  }
}