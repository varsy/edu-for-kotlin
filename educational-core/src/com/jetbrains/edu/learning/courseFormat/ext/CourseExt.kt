@file:JvmName("CourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.codeforces.api.ContestInfo
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse

val Course.configurator: EduConfigurator<*>?
  get() {
    val language = languageById ?: return null
    return EduConfiguratorManager.findConfigurator(itemType, environment, language)
  }

val Course.compatibilityProvider: CourseCompatibilityProvider?
  get() {
    return CourseCompatibilityProviderEP.find(languageID, environment)
  }

val Course.sourceDir: String? get() = configurator?.sourceDir
val Course.testDirs: List<String> get() = configurator?.testDirs.orEmpty()

val Course.project: Project?
  get() {
    for (project in ProjectManager.getInstance().openProjects) {
      if (this == StudyTaskManager.getInstance(project).course) {
        return project
      }
    }
    return null
  }

val Course.hasSections: Boolean get() = sections.isNotEmpty()

val Course.hasTopLevelLessons: Boolean get() = lessons.isNotEmpty()

val Course.allTasks: List<Task>
  get() {
    val allTasks = mutableListOf<Task>()
    course.visitTasks { allTasks += it }
    return allTasks
  }

val Course.languageDisplayName: String get() = languageById?.displayName ?: languageID

val Course.technologyName: String?
  get() = compatibilityProvider?.technologyName ?: languageById?.displayName

val Course.supportedTechnologies: List<String>
  get() {
    return when (this) {
      is JetBrainsAcademyCourse -> this.supportedLanguages
      else -> if (technologyName != null) listOf(technologyName!!) else emptyList()
    }
  }

val Course.tags: List<Tag>
  get() {
    if (course is CodeforcesCourse) {
      return emptyList()
    }

    val tags = mutableListOf<Tag>()
    if (course is JetBrainsAcademyCourse) {
      tags.addAll((this as JetBrainsAcademyCourse).supportedLanguages.map { ProgrammingLanguageTag(it) })
      tags.add(HumanLanguageTag(humanLanguage))
      return tags
    }

    technologyName?.let { tags.add(ProgrammingLanguageTag(it)) }
    tags.add(HumanLanguageTag(humanLanguage))

    if (course is EduCourse) {
      if (visibility is CourseVisibility.FeaturedVisibility) {
        tags.add(FeaturedTag())
      }
    }
    return tags
  }

val Course.languageById: Language?
  get() = Language.findLanguageByID(EduLanguage.get(programmingLanguage).id)


val Course.isPreview: Boolean
  get() = this is EduCourse && isPreview

val Course.compatibility: CourseCompatibility
  get() {
    if (this is JetBrainsAcademyCourse || this is CodeforcesCourse || this is ContestInfo) {
      return CourseCompatibility.Compatible
    }

    // @formatter:off
    return versionCompatibility() ?:
           pluginCompatibility() ?:
           configuratorCompatibility() ?:
           CourseCompatibility.Compatible
    // @formatter:on
  }

private fun Course.versionCompatibility(): CourseCompatibility? {
  if (this !is EduCourse) {
    return null
  }

  if (programmingLanguage.isEmpty()) {
    return CourseCompatibility.Unsupported
  }

  if (formatVersion > JSON_FORMAT_VERSION) {
    return CourseCompatibility.IncompatibleVersion
  }

  return null
}

// projectLanguage parameter should be passed only for hyperskill courses because for Hyperskill
// it can differ from the course.programmingLanguage
fun Course.validateLanguage(projectLanguage: String = programmingLanguage): Result<Unit, CourseValidationResult> {
  val pluginCompatibility = pluginCompatibility()
  if (pluginCompatibility is CourseCompatibility.PluginsRequired) {
    return Err(PluginsRequired(pluginCompatibility.toInstallOrEnable))
  }

  if (configurator == null) {
    val message = EduCoreBundle.message(
      "rest.service.language.not.supported", ApplicationNamesInfo.getInstance().productName,
      projectLanguage.capitalize()
    )
    return Err(ValidationErrorMessage(message))
  }
  return Ok(Unit)
}

private fun Course.pluginCompatibility(): CourseCompatibility? {
  val requiredPlugins = mutableListOf<PluginInfo>()
  compatibilityProvider?.requiredPlugins()?.let { requiredPlugins.addAll(it) }
  for (pluginInfo in course.pluginDependencies) {
    if (requiredPlugins.find { it.stringId == pluginInfo.stringId } != null) {
      continue
    }
    requiredPlugins.add(pluginInfo)
  }

  if (requiredPlugins.isEmpty()) {
    return null
  }

  val pluginsState = InstalledPluginsState.getInstance()

  // TODO: O(requiredPlugins * allPlugins) because PluginManager.getPlugin takes O(allPlugins).
  //  Can be improved at least to O(requiredPlugins * log(allPlugins))
  val loadedPlugins = PluginManager.getLoadedPlugins()
  val notLoadedPlugins = requiredPlugins
    .mapNotNull {
      if (pluginsState.wasInstalledWithoutRestart(PluginId.getId(it.stringId))) {
        return@mapNotNull null
      }

      val pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(it.stringId))
      if (pluginDescriptor == null || pluginDescriptor !in loadedPlugins) {
        it to pluginDescriptor
      }
      else {
        null
      }
    }

  val toInstallOrEnable = notLoadedPlugins.filter { (info, pluginDescriptor) ->
    // Plugin is just installed and not loaded by IDE (i.e. it requires restart)
    pluginDescriptor == null && !pluginsState.wasInstalled(PluginId.getId(info.stringId)) ||
    // Plugin is installed but disabled
    pluginDescriptor?.isEnabled == false
  }

  return if (notLoadedPlugins.isNotEmpty()) CourseCompatibility.PluginsRequired(toInstallOrEnable.map { it.first }) else null
}

private fun Course.configuratorCompatibility(): CourseCompatibility? {
  return if (configurator == null) CourseCompatibility.Unsupported else null
}

fun Course.updateEnvironmentSettings(project: Project, configurator: EduConfigurator<*>? = this.configurator) {
  configurator ?: return
  course.environmentSettings = configurator.getEnvironmentSettings(project)
}

fun Course.visitEduFiles(visitor: (EduFile) -> Unit) {
  visitTasks { task ->
    for (taskFile in task.taskFiles.values) {
      visitor(taskFile)
    }
  }

  for (additionalFile in additionalFiles) {
    visitor(additionalFile)
  }
}