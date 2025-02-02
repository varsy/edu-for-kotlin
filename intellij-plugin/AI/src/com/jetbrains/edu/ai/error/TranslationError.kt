package com.jetbrains.edu.ai.error

sealed class TranslationError(messageKey: String) : AIServiceError(messageKey) {
  data object NoTranslation : TranslationError("ai.translation.course.translation.does.not.exist")
  data object TranslationUnavailableForLegalReasons : TranslationError("ai.translation.translation.unavailable.due.to.license.restrictions")
}