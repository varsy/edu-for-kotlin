package com.jetbrains.edu.ai.hints.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.jetbrains.educational.ml.hints.hint.Hint
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.lang.reflect.Type

class HintsConverterFactory(private val objectMapper: ObjectMapper = jacksonObjectMapper()) : Converter.Factory() {
  override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, List<Hint>> {
    return Converter<ResponseBody, List<Hint>> { body ->
      val responseList = mutableListOf<Hint?>()
      body.byteStream().use { resource ->
        resource.bufferedReader().useLines { lines ->
          for (line in lines) {
            val value = try {
              objectMapper.readValue<Hint>(line)
            } catch (_: Throwable) {
              null
            }
            responseList.add(value)
          }
        }
      }
      responseList.filterNotNull()
    }
  }

  override fun requestBodyConverter(
    type: Type,
    parameterAnnotations: Array<out Annotation>,
    methodAnnotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<*, RequestBody>? {
    return JacksonConverterFactory.create(objectMapper).requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
  }
}