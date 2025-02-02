package com.jetbrains.edu.ai.translation.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.translation.settings.TranslationSettings
import com.jetbrains.edu.ai.translation.statistics.EduAITranslationEventFields.ALWAYS_TRANSLATE_FIELD
import com.jetbrains.edu.ai.translation.statistics.EduAITranslationEventFields.ORIGINAL_LANG_FIELD
import com.jetbrains.edu.ai.translation.statistics.EduAITranslationEventFields.TRANSLATION_ERROR_FIELD
import com.jetbrains.edu.ai.translation.statistics.EduAITranslationEventFields.TRANSLATION_LANG_FIELD
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_ID_FIELD
import com.jetbrains.edu.learning.statistics.EventLogGroup
import com.jetbrains.edu.learning.statistics.registerEvent
import com.jetbrains.educational.core.format.enum.TranslationLanguage

class EduAITranslationCounterUsageCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  @Suppress("CompanionObjectInExtension")
  companion object {
    private val GROUP = EventLogGroup(
      "educational.ai.features",
      "The metric is reported in case a user has called the corresponding JetBrains Academy AI features.",
      1,
    )

    private val TRANSLATION_BUTTON_CLICKED_EVENT = GROUP.registerEvent(
      "translation.button.clicked",
      "The event is recorded when the translation button is clicked.",
      COURSE_ID_FIELD
    )
    private val TRANSLATION_DISABLED_EVENT = GROUP.registerEvent(
      "translation.disabled",
      "The event is recorded when the course translation is disabled.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
    )
    private val TRANSLATION_FINISHED_EVENT = GROUP.registerVarargEvent(
      "translation.finished",
      "The event is recorded when the translation is successfully completed.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
      ALWAYS_TRANSLATE_FIELD
    )
    private val TRANSLATION_FINISHED_WITH_ERROR_EVENT = GROUP.registerVarargEvent(
      "translation.finished.with.error",
      "The event is recorded in case an error occurs during translation.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
      TRANSLATION_ERROR_FIELD
    )
    private val TRANSLATION_LANGUAGE_PICKER_OPENED_EVENT = GROUP.registerEvent(
      "translation.language.picker.opened",
      "The event is recorded when the language picker is opened by the user.",
      COURSE_ID_FIELD
    )
    private val TRANSLATION_RETRIED_EVENT = GROUP.registerVarargEvent(
      "translation.retried",
      "The event is recorded when a retry of the translation process is initiated.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
      TRANSLATION_ERROR_FIELD,
    )
    private val TRANSLATION_STARTED_EVENT = GROUP.registerVarargEvent(
      "translation.started",
      "The event is recorded when the translation process starts.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
      ALWAYS_TRANSLATE_FIELD,
    )
    private val TRANSLATION_UPDATED_EVENT = GROUP.registerEvent(
      "translation.updated",
      "The event is recorded when the translation is successfully updated.",
      COURSE_ID_FIELD,
      ORIGINAL_LANG_FIELD,
      TRANSLATION_LANG_FIELD,
    )

    fun translationButtonClicked(course: EduCourse) = TRANSLATION_BUTTON_CLICKED_EVENT.log(course.id)

    fun translationDisabled(course: EduCourse, translationLanguage: TranslationLanguage) =
      TRANSLATION_DISABLED_EVENT.log(course.id, course.languageCode, translationLanguage)

    fun translationFinishedSuccessfully(course: EduCourse, translationLanguage: TranslationLanguage) =
      TRANSLATION_FINISHED_EVENT.log(
        COURSE_ID_FIELD.with(course.id),
        ORIGINAL_LANG_FIELD.with(course.languageCode),
        TRANSLATION_LANG_FIELD.with(translationLanguage),
        alwaysTranslate()
      )

    fun translationFinishedWithError(
      course: EduCourse,
      translationLanguage: TranslationLanguage,
      translationError: AIServiceError
    ) = TRANSLATION_FINISHED_WITH_ERROR_EVENT.log(
      COURSE_ID_FIELD.with(course.id),
      ORIGINAL_LANG_FIELD.with(course.languageCode),
      TRANSLATION_LANG_FIELD.with(translationLanguage),
      TRANSLATION_ERROR_FIELD.with(translationError.toStatisticsFormat())
    )

    fun translationLanguagePickerOpened(course: EduCourse) = TRANSLATION_LANGUAGE_PICKER_OPENED_EVENT.log(course.id)

    fun translationRetried(course: EduCourse, translationLanguage: TranslationLanguage, translationError: AIServiceError) =
      TRANSLATION_RETRIED_EVENT.log(
        COURSE_ID_FIELD.with(course.id),
        ORIGINAL_LANG_FIELD.with(course.languageCode),
        TRANSLATION_LANG_FIELD.with(translationLanguage),
        TRANSLATION_ERROR_FIELD.with(translationError.toStatisticsFormat()),
      )

    fun translationStarted(course: EduCourse, translationLanguage: TranslationLanguage) =
      TRANSLATION_STARTED_EVENT.log(
        COURSE_ID_FIELD.with(course.id),
        ORIGINAL_LANG_FIELD.with(course.languageCode),
        TRANSLATION_LANG_FIELD.with(translationLanguage),
        alwaysTranslate(),
      )

    fun translationUpdated(course: EduCourse, translationLanguage: TranslationLanguage) =
      TRANSLATION_UPDATED_EVENT.log(course.id, course.languageCode, translationLanguage)

    private fun alwaysTranslate(): EventPair<Boolean> {
      val autoTranslate = TranslationSettings.getInstance().autoTranslate
      return ALWAYS_TRANSLATE_FIELD.with(autoTranslate)
    }
  }
}