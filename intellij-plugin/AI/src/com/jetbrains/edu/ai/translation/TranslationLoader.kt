package com.jetbrains.edu.ai.translation

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.AIServiceLoader
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.ai.translation.settings.TranslationSettings
import com.jetbrains.edu.ai.translation.statistics.EduAITranslationCounterUsageCollector
import com.jetbrains.edu.ai.translation.ui.AITranslationNotification.ActionLabel
import com.jetbrains.edu.ai.translation.ui.AITranslationNotificationManager
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.fileNameWithTranslation
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getTaskDirectory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.CourseTranslationResponse
import com.jetbrains.educational.translation.format.TranslatedText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.sync.Mutex
import java.io.IOException

@Service(Service.Level.PROJECT)
class TranslationLoader(private val project: Project, private val scope: CoroutineScope) {
  private val mutex = Mutex()

  init {
    scope.launch {
      TranslationSettings.getInstance().autoTranslationSettings.collectLatest { properties ->
        if (properties == null) return@collectLatest
        val course = project.course as? EduCourse ?: return@collectLatest
        if (TranslationProjectSettings.isCourseTranslated(project)) return@collectLatest
        if (isRunning(project)) return@collectLatest
        if (properties.autoTranslate) {
          val language = properties.language
          if (!language.isSameLanguage(course)) {
            fetchAndApplyTranslation(course, language)
          }
        }
      }
    }
  }

  fun fetchAndApplyTranslation(course: EduCourse, translationLanguage: TranslationLanguage) {
    if (translationLanguage.isSameLanguage(course)) {
      LOG.warn("Translation language ${translationLanguage.code} matches the course language ${course.languageCode}")
      return
    }
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.already.running")) {
      doFetchAndApplyTranslation(course, translationLanguage)
    }
  }

  fun updateTranslation(course: EduCourse, translationProperties: TranslationProperties) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.update.is.not.possible")) {
      doUpdateTranslation(course, translationProperties)
    }
  }

  fun updateTranslationWhenCourseUpdate(course: EduCourse) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.update.is.not.possible")) {
      val properties = TranslationProjectSettings.getInstance(project).translationProperties.value
      doResetCourseTranslation(course)
      if (properties != null) {
        doUpdateTranslation(course, properties)
      }
    }
  }

  fun resetCourseTranslation(course: EduCourse) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.reset.is.not.possible")) {
      doResetCourseTranslation(course)
    }
  }

  private suspend fun doFetchAndApplyTranslation(course: EduCourse, translationLanguage: TranslationLanguage): Unit =
    withBackgroundProgress(project, EduAIBundle.message("ai.translation.getting.course.translation")) {
      val translationSettings = TranslationProjectSettings.getInstance(project)

      if (course.isTranslationExists(translationLanguage)) {
        val properties = translationSettings.getTranslationPropertiesByLanguage(translationLanguage)
        if (properties != null) {
          translationSettings.setTranslation(properties)
          return@withBackgroundProgress
        }
      }

      EduAITranslationCounterUsageCollector.translationStarted(course, translationLanguage)
      val translation = fetchTranslation(course, translationLanguage).onError { error ->
        EduAITranslationCounterUsageCollector.translationFinishedWithError(course, translationLanguage, error)
        LOG.warn("Failed to get translation for ${course.name} to $translationLanguage: $error")
        return@withBackgroundProgress
      }
      course.saveTranslation(translation)
      translationSettings.setTranslation(translation.toTranslationProperties())
      EduAITranslationCounterUsageCollector.translationFinishedSuccessfully(course, translationLanguage)
    }

  private suspend fun doUpdateTranslation(course: EduCourse, translationProperties: TranslationProperties): Unit =
    withBackgroundProgress(project, EduAIBundle.message("ai.translation.update.course.translation")) {
      val (language, _, version) = translationProperties
      val translation = fetchTranslation(course, language).onError { error ->
        EduAITranslationCounterUsageCollector.translationFinishedWithError(course, language, error)
        LOG.warn("Failed to update translation for ${course.name} to $language: $error")
        return@withBackgroundProgress
      }
      val translationSettings = TranslationProjectSettings.getInstance(project)
      if (version == translation.version) {
        AITranslationNotificationManager.showInfoNotification(
          project,
          message = EduAIBundle.message("ai.translation.translation.is.up.to.date")
        )
        return@withBackgroundProgress
      }
      course.saveTranslation(translation)
      translationSettings.setTranslation(translation.toTranslationProperties())
      AITranslationNotificationManager.showInfoNotification(
        project,
        message = EduAIBundle.message("ai.translation.translation.has.been.updated")
      )
      EduAITranslationCounterUsageCollector.translationUpdated(course, translation.language)
    }

  private suspend fun doResetCourseTranslation(course: EduCourse) {
    if (TranslationProjectSettings.isCourseTranslated(project)) {
      withBackgroundProgress(project, EduAIBundle.message("ai.translation.reset.course.translation")) {
        TranslationProjectSettings.getInstance(project).resetTranslation()
      }
    }
    withBackgroundProgress(project, EduAIBundle.message("ai.translation.deleting.course.translation.files")) {
      course.deleteAllTranslations()
    }
  }

  private inline fun runInBackgroundExclusively(
    @NotificationContent lockNotAcquiredNotificationText: String,
    crossinline action: suspend () -> Unit
  ) {
    scope.launch {
      if (mutex.tryLock()) {
        try {
          action()
        }
        finally {
          mutex.unlock()
        }
      }
      else {
        AITranslationNotificationManager.showErrorNotification(project, message = lockNotAcquiredNotificationText)
      }
    }
  }

  private suspend fun fetchTranslation(
    course: EduCourse,
    language: TranslationLanguage
  ): Result<CourseTranslationResponse, AIServiceError> =
    withContext(Dispatchers.IO) {
      val translation = downloadTranslation(course, language)
      if (translation is Err) {
        val actionLabel = ActionLabel(
          name = EduCoreBundle.message("retry"),
          action = {
            EduAITranslationCounterUsageCollector.translationRetried(course, language, translation.error)
            fetchAndApplyTranslation(course, language)
          }
        )
        AITranslationNotificationManager.showErrorNotification(project, message = translation.error.message(), actionLabel = actionLabel)
      }
      return@withContext translation
    }

  @OptIn(ExperimentalStdlibApi::class)
  private suspend fun EduCourse.deleteAllTranslations() = allTasks.map {
    scope.async(Dispatchers.IO) {
      TranslationLanguage.entries.forEach { language ->
        it.deleteTranslation(language)
      }
    }
  }.awaitAll()

  private suspend fun EduCourse.isTranslationExists(translationLanguage: TranslationLanguage): Boolean =
    readAction {
      allTasks.all {
        it.getDescriptionFile(project, translationLanguage = translationLanguage)?.exists() == true
      }
    }

  private suspend fun downloadTranslation(
    course: EduCourse,
    language: TranslationLanguage
  ): Result<CourseTranslationResponse, AIServiceError> =
    TranslationServiceConnector.getInstance().getTranslatedCourse(course.id, course.marketplaceCourseVersion, language)

  private suspend fun EduCourse.saveTranslation(courseTranslation: CourseTranslationResponse) {
    val taskDescriptions = courseTranslation.taskDescriptions
    for (task in allTasks) {
      val translation = taskDescriptions[task.id.toString()] ?: continue
      task.saveTranslation(translation)
    }
  }

  private suspend fun Task.saveTranslation(text: TranslatedText) {
    val taskDirectory = readAction { getTaskDirectory(project) } ?: return
    val name = descriptionFormat.fileNameWithTranslation(text.language)

    try {
      blockingContext {
        GeneratorUtils.createTextChildFile(project, taskDirectory, name, text.text)
      }
    }
    catch (exception: IOException) {
      LOG.error("Failed to write text to $taskDirectory", exception)
      throw exception
    }
  }

  private suspend fun Task.deleteTranslation(translationLanguage: TranslationLanguage) {
    try {
      val translationFile = readAction {
        getDescriptionFile(project, translationLanguage = translationLanguage)
      } ?: return
      writeAction {
        translationFile.delete(this::class.java)
      }
    }
    catch (exception: IOException) {
      LOG.error("Failed to delete ${translationLanguage.label} translation file", exception)
      throw exception
    }
  }

  private fun CourseTranslationResponse.toTranslationProperties(): TranslationProperties =
    TranslationProperties(language, itemNames, version)

  companion object {
    private val LOG = Logger.getInstance(TranslationLoader::class.java)

    fun getInstance(project: Project): TranslationLoader = project.service()

    fun isRunning(project: Project): Boolean = getInstance(project).mutex.isLocked
  }
}