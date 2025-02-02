package com.jetbrains.edu.ai.translation.updater

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.ai.translation.ui.AITranslationNotification.ActionLabel
import com.jetbrains.edu.ai.translation.ui.AITranslationNotificationManager
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError

@Service(Service.Level.PROJECT)
class TranslationUpdateChecker(private val project: Project) {
  suspend fun checkUpdate(course: EduCourse) {
    val translationProperties = TranslationProjectSettings.getInstance(project).translationProperties.value
    if (translationProperties != null && isTranslationOutdated(course, translationProperties)) {
      showUpdateAvailableNotification {
        TranslationLoader.getInstance(project).updateTranslation(course, translationProperties)
      }
    }
  }

  private suspend fun isTranslationOutdated(course: EduCourse, translationProperties: TranslationProperties): Boolean {
    val (language, _, version) = translationProperties
    val latestVersion = TranslationServiceConnector.getInstance()
      .getLatestTranslationVersion(course.id, course.marketplaceCourseVersion, language)
      .onError {
        LOG.error(it.message())
        return false
      }
    return version != latestVersion
  }

  private fun showUpdateAvailableNotification(updateAction: () -> Unit) {
    val actionLabel = ActionLabel(
      name = EduCoreBundle.message("update.action"),
      action = updateAction
    )
    AITranslationNotificationManager.showInfoNotification(
      project,
      message = EduAIBundle.message("ai.translation.an.updated.version.of.the.translation.is.available"),
      actionLabel = actionLabel
    )
  }

  companion object {
    private val LOG = Logger.getInstance(TranslationUpdateChecker::class.java)

    fun getInstance(project: Project): TranslationUpdateChecker = project.service()
  }
}