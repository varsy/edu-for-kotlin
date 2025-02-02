package com.jetbrains.edu.ai.terms.connector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.ai.connector.AIServiceConnector
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.error.TermsServiceError
import com.jetbrains.edu.ai.terms.service.TermsService
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.network.HTTP_UNAVAILABLE_FOR_LEGAL_REASONS
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import com.jetbrains.educational.terms.format.CourseTermsResponse
import com.jetbrains.educational.terms.format.domain.TermsVersion
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.HttpURLConnection.*

@Service(Service.Level.APP)
class TermsServiceConnector : AIServiceConnector() {
  override val service: TermsService
    get() = termsService()

  private fun termsService(): TermsService {
    val objectMapper = jacksonObjectMapper()
    val factory = JacksonConverterFactory.create(objectMapper)
    return createRetrofitBuilder(aiServiceUrl, connectionPool)
      .addConverterFactory(factory)
      .build()
      .create(TermsService::class.java)
  }

  suspend fun getLatestTermsVersion(
    marketplaceId: Int,
    updateVersion: Int,
    language: TranslationLanguage
  ): Result<TermsVersion, AIServiceError> =
    networkCall {
      service
        .getLatestCourseTermsVersion(marketplaceId, updateVersion, language.name)
        .handleResponse()
    }

  suspend fun getCourseTerms(
    marketplaceId: Int,
    updateVersion: Int,
    language: TranslationLanguage
  ): Result<CourseTermsResponse, AIServiceError> =
    networkCall {
      service
        .getCourseTerms(marketplaceId, updateVersion, language.name)
        .handleResponse()
    }

  override fun <T> parseResponseCode(code: Int, result: T?): Result<T, AIServiceError> {
    return when {
      code == HTTP_OK && result != null -> Ok(result)
      code == HTTP_UNAVAILABLE || result == null -> AIServiceError.ServiceUnavailable.asErr()
      code == HTTP_NOT_FOUND -> TermsServiceError.NoTerms.asErr()
      code == HTTP_UNAVAILABLE_FOR_LEGAL_REASONS -> TermsServiceError.TermsUnavailableForLegalReason.asErr()
      else -> AIServiceError.ConnectionError.asErr ()
    }
  }

  companion object {
    fun getInstance(): TermsServiceConnector = service()
  }
}