package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkLanguageSettings
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduNames.PROJECT_NAME
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jetbrains.annotations.NonNls
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME

abstract class GradleCourseBuilderBase : EduCourseBuilder<JdkProjectSettings> {

  abstract fun buildGradleTemplateName(course: Course): String
  open fun settingGradleTemplateName(course: Course): String = SETTINGS_FILE_NAME

  /**
   * Map from config file name which should be created in project to template file name
   */
  fun templates(course: Course): Map<String, String> = mapOf(
    DEFAULT_SCRIPT_NAME to buildGradleTemplateName(course),
    SETTINGS_FILE_NAME to settingGradleTemplateName(course)
  )

  open fun templateVariables(projectName: String): Map<String, Any> {
    return mapOf(PROJECT_NAME to GeneratorUtils.gradleSanitizeName(projectName)) +
           getKotlinTemplateVariables() +
           getJarvisTemplateVariables()
  }

  override fun refreshProject(project: Project, cause: RefreshCause) {
    GradleCourseRefresher.firstAvailable()?.refresh(project, cause)
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = JdkLanguageSettings()

  override fun getDefaultSettings(): Result<JdkProjectSettings, String> = JdkProjectSettings.defaultSettings()

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator =
    GradleCourseProjectGenerator(this, course)

  companion object {
    @NonNls
    const val HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME: String = "hyperskill-settings.gradle"

    fun getKotlinTemplateVariables(): Map<String, Any> {
      val kotlinVersion = kotlinVersion()
      return mapOf(
        "KOTLIN_VERSION" to kotlinVersion.version
      )
    }

    fun getJarvisTemplateVariables() = mapOf(
      "isJarvis" to System.getProperty("is.jarvis.mode", "isJarvis").toBoolean()
    )
  }
}
