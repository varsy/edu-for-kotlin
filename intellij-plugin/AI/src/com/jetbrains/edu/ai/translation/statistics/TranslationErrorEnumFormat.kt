package com.jetbrains.edu.ai.translation.statistics

import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.error.TranslationError

enum class TranslationErrorEnumFormat {
  CONNECTION_ERROR,
  NO_TRANSLATION,
  SERVICE_UNAVAILABLE,
  TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS;
}

fun AIServiceError.toStatisticsFormat(): TranslationErrorEnumFormat = when (this) {
  is AIServiceError.ConnectionError -> TranslationErrorEnumFormat.CONNECTION_ERROR
  is AIServiceError.ServiceUnavailable -> TranslationErrorEnumFormat.SERVICE_UNAVAILABLE
  is TranslationError.NoTranslation -> TranslationErrorEnumFormat.NO_TRANSLATION
  is TranslationError.TranslationUnavailableForLegalReasons -> TranslationErrorEnumFormat.TRANSLATION_UNAVAILABLE_FOR_LEGAL_REASONS
  else -> error("Unexpected error type for translation: $this")
}
