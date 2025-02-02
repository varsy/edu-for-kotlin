package com.jetbrains.edu.ai.error

sealed class TermsServiceError(messageKey: String) : AIServiceError(messageKey) {
  data object NoTerms : TermsServiceError("ai.terms.course.terms.does.not.exist")
  data object TermsUnavailableForLegalReason : TermsServiceError("ai.terms.terms.unavailable.due.to.license.restrictions")
}