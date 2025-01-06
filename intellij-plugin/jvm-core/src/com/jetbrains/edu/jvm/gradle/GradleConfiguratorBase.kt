package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.jvmEnvironmentSettings
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configuration.ArchiveFileInfo
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.IncludeType
import com.jetbrains.edu.learning.configuration.buildArchiveFileInfo
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_JAR
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_WIN
import com.jetbrains.edu.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import com.jetbrains.edu.learning.gradle.GradleConstants.SETTINGS_GRADLE

abstract class GradleConfiguratorBase : EduConfigurator<JdkProjectSettings> {
  abstract override val courseBuilder: GradleCourseBuilderBase

  override fun archiveFileInfo(holder: CourseInfoHolder<out Course?>, file: VirtualFile): ArchiveFileInfo =
    buildArchiveFileInfo(holder, file) {
      when {
        nameRegex("""^$SETTINGS_GRADLE$""") -> {
          description("Gradle settings")
          type(IncludeType.MUST_INCLUDE)
          includeInArchive()
        }

        FOLDERS_TO_EXCLUDE.any { regex("""(^|/)$it(/|$)""") } -> {
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        file.name in NAMES_TO_EXCLUDE -> {
          type(IncludeType.MUST_NOT_INCLUDE)
        }

        else -> info(super.archiveFileInfo(holder, file))
      }
    }

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("org.jetbrains.plugins.gradle"), PluginId.getId("JUnit"))

  companion object {
    private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
      "EduTestRunner.java", GRADLE_WRAPPER_UNIX, GRADLE_WRAPPER_WIN, LOCAL_PROPERTIES,
      SETTINGS_GRADLE, GRADLE_WRAPPER_JAR, GRADLE_WRAPPER_PROPERTIES
    )

    private val FOLDERS_TO_EXCLUDE = ContainerUtil.newHashSet(EduNames.OUT, EduNames.BUILD, GRADLE)
  }

  override fun getEnvironmentSettings(project: Project): Map<String, String> = jvmEnvironmentSettings(project)
}
