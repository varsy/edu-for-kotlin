package com.jetbrains.edu.javascript.learning

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.javascript.learning.checker.JsTaskCheckerProvider
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.ArchiveFileInfo
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.IncludeType
import com.jetbrains.edu.learning.configuration.buildArchiveFileInfo
import com.jetbrains.edu.learning.courseFormat.Course
import javax.swing.Icon


open class JsConfigurator : EduConfigurator<JsNewProjectSettings> {
  override val courseBuilder: EduCourseBuilder<JsNewProjectSettings>
    get() = JsCourseBuilder()

  override val testFileName: String
    get() = ""

  override fun getMockFileName(course: Course, text: String): String = TASK_JS

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val taskCheckerProvider: TaskCheckerProvider
    get() = JsTaskCheckerProvider()

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("NodeJS"))

  override val logo: Icon
    get() = EducationalCoreIcons.Language.JavaScript

  override val isEnabled: Boolean
    get() = !PlatformUtils.isRider()

  override fun archiveFileInfo(holder: CourseInfoHolder<out Course?>, file: VirtualFile): ArchiveFileInfo =
    buildArchiveFileInfo(holder, file) {
      when {
        regex("(^|/)node_modules(/|$)") -> {
          description("Node packages")
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        nameRegex("^package-lock.json$") -> {
          description("Package lock file")
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        else -> info(super.archiveFileInfo(holder, file))
      }
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val MAIN_JS = "main.js"
    const val TASK_JS = "task.js"
    const val TEST_JS = "test.js"
  }
}
