package com.jetbrains.edu.ai.hints.service

import com.jetbrains.educational.ml.hints.context.CodeHintContext
import com.jetbrains.educational.ml.hints.context.TextHintContext
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface HintsService {
  @POST("$API_HINTS/code")
  suspend fun getCodeHint(@Body context: CodeHintContext): Response<List<CodeHint>>

  @POST("$API_HINTS/text")
  suspend fun getTextHint(@Body context: TextHintContext): Response<List<TextHint>>

  companion object {
    private const val API_HINTS = "/api/hints"
  }
}