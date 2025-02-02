package com.jetbrains.edu.ai.translation.statistics

import com.intellij.internal.statistic.eventLog.events.EventFields
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import java.util.*

object EduAITranslationEventFields {
  private const val ALWAYS_TRANSLATE: String = "always_translate"
  private const val ORIGINAL_LANG: String = "original_lang"
  private const val TRANSLATION_ERROR: String = "translation_error"
  private const val TRANSLATION_LANG: String = "translation_lang"

  private val allowedLanguages = Locale.getISOLanguages().map { Locale(it).toLanguageTag() }

  val ALWAYS_TRANSLATE_FIELD = EventFields.Boolean(ALWAYS_TRANSLATE)
  val ORIGINAL_LANG_FIELD = EventFields.String(ORIGINAL_LANG, allowedLanguages)
  val TRANSLATION_ERROR_FIELD = EventFields.Enum<TranslationErrorEnumFormat>(TRANSLATION_ERROR) { it.name.lowercase(Locale.getDefault()) }
  val TRANSLATION_LANG_FIELD = EventFields.Enum<TranslationLanguage>(TRANSLATION_LANG) { it.code }
}