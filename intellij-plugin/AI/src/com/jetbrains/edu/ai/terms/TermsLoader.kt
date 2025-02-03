package com.jetbrains.edu.ai.terms

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.terms.connector.TermsServiceConnector
import com.jetbrains.edu.ai.terms.settings.TheoryLookupSettings
import com.jetbrains.edu.ai.translation.ui.AITranslationNotification.ActionLabel
import com.jetbrains.edu.ai.translation.ui.AITranslationNotificationManager
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import com.jetbrains.edu.learning.ai.terms.TermsProperties
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.CourseTermsResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class TermsLoader(private val project: Project, private val scope: CoroutineScope) {
  private val mutex = Mutex()

  val isRunning: Boolean
    get() = mutex.isLocked

  init {
    scope.launch {
      TheoryLookupSettings.getInstance().theoryLookupProperties.collectLatest { properties ->
        if (properties == null) return@collectLatest
        val course = project.course as? EduCourse ?: return@collectLatest
        if (TermsProjectSettings.areCourseTermsLoaded(project)) return@collectLatest
        if (isRunning) return@collectLatest
        if (properties.isEnabled) {
          val language = TranslationProjectSettings.getInstance(project).translationProperties.value?.language
          if (language?.code == TranslationLanguage.ENGLISH.code) { // TODO(omit it in the future)
            fetchAndApplyTerms(course, language)
          }
        }
      }
    }
  }

  fun updateTerms(course: EduCourse, termsProperties: TermsProperties) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.update.is.not.possible")) {
      doUpdateTerms(course, termsProperties)
    }
  }

  fun updateTermsWhenCourseUpdate(course: EduCourse) {
    runInBackgroundExclusively(EduAIBundle.message("ai.translation.update.is.not.possible")) {
      val termsProperties = TermsProjectSettings.getInstance(project).termsProperties.value
      doResetTerms()
      if (termsProperties != null) {
        doUpdateTerms(course, termsProperties)
      }
    }
  }

  fun fetchAndApplyTerms(course: EduCourse, language: TranslationLanguage?) {
    if (language?.code != TranslationLanguage.ENGLISH.code) {
      LOG.warn("Language ${language?.code} requested for theory lookup is not English")
      return
    }
    runInBackgroundExclusively(EduAIBundle.message("ai.terms.already.running")) {
      doFetchAndApplyTerms(course, language)
    }
  }

  fun resetTerms() {
    runInBackgroundExclusively(EduAIBundle.message("ai.terms.reset.is.not.possible")) {
      doResetTerms()
    }
  }

  private suspend fun doResetTerms() {
    if (TermsProjectSettings.areCourseTermsLoaded(project)) {
      withBackgroundProgress(project, EduAIBundle.message("ai.terms.reset.course.terms")) {
        TermsProjectSettings.getInstance(project).resetTerms()
      }
    }
  }

  private suspend fun doFetchAndApplyTerms(course: EduCourse, language: TranslationLanguage) {
    withBackgroundProgress(project, EduAIBundle.message("ai.terms.getting.course.terms")) {
      val termsProjectSettings = TermsProjectSettings.getInstance(project)
      val properties = termsProjectSettings.getTermsByLanguage(language)
      if (properties != null) {
        termsProjectSettings.setTerms(properties)
        return@withBackgroundProgress
      }
      //TODO(add statistics (fetch started))
      val termsResponse = fetchTerms(course, language).onError { error ->
        //TODO(add statistics (fetch failed))
        LOG.warn("Failed to get terms for ${course.name} to $language: $error")
        return@withBackgroundProgress
      }
      termsProjectSettings.setTerms(termsResponse.toTermsProperties())
      //TODO(add statistics (fetch finished))
    }
  }

  private suspend fun doUpdateTerms(course: EduCourse, termsProperties: TermsProperties) {
    withBackgroundProgress(project, EduAIBundle.message("ai.terms.update.course.terms")) {
      val (language, _, version) = termsProperties
      //TODO(add statistics (update started))
      val termsResponse = fetchTerms(course, language).onError { error ->
        //TODO(add statistics (update failed))
        LOG.warn("Failed to update terms for ${course.name} to $language: $error")
        return@withBackgroundProgress
      }
      val termsProjectSettings = TermsProjectSettings.getInstance(project)
      if (version == termsResponse.termsVersion) {
        AITranslationNotificationManager.showInfoNotification(
          project,
          message = EduAIBundle.message("ai.terms.terms.is.up.to.date")
        )
        return@withBackgroundProgress
      }
      termsProjectSettings.setTerms(termsResponse.toTermsProperties())
      AITranslationNotificationManager.showInfoNotification(
        project,
        message = EduAIBundle.message("ai.terms.terms.has.been.updated")
      )
      //TODO(add statistics (update finished))
    }
  }

  private suspend fun fetchTerms(
    course: EduCourse,
    language: TranslationLanguage
  ): Result<CourseTermsResponse, AIServiceError> {
    return withContext(Dispatchers.IO) {
      val courseTerms = downloadTerms(course, language)
      if (courseTerms is Err) {
        val actionLabel = ActionLabel(
          name = EduCoreBundle.message("retry"),
          action = {
            // TODO(add statistics (fetch failed))
            fetchAndApplyTerms(course, language)
          }
        )
        AITranslationNotificationManager.showErrorNotification(project, message = courseTerms.error.message(), actionLabel = actionLabel)
      }
      courseTerms
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

  private suspend fun downloadTerms(course: EduCourse, language: TranslationLanguage): Result<CourseTermsResponse, AIServiceError> {
    return TermsServiceConnector.getInstance().getCourseTerms(
      course.id,
      course.marketplaceCourseVersion,
      language
    )
  }

  private fun CourseTermsResponse.toTermsProperties(): TermsProperties =
    TermsProperties(language, terms.mapKeys { it.key.toInt() }, termsVersion) // TODO(fix terms in ai format)

  companion object {
    private val LOG = Logger.getInstance(TermsLoader::class.java)

    fun getInstance(project: Project): TermsLoader = project.service()
  }
}