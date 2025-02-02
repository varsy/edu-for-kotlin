package com.jetbrains.edu.ai.connector

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.ai.error.AIServiceError
import com.jetbrains.edu.ai.host.EduAIServiceHost
import com.jetbrains.edu.learning.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import retrofit2.Response
import java.net.SocketException

abstract class AIServiceConnector : Disposable {
  protected val aiServiceUrl: String
    get() = EduAIServiceHost.getSelectedUrl()

  protected val connectionPool = ConnectionPool()

  abstract val service: AIService

  override fun dispose() {
    connectionPool.evictAll()
  }

  protected fun <T> Response<T>.handleResponse(): Result<T, AIServiceError> {
    val code = code()
    val errorMessage = errorBody()?.string()
    if (!errorMessage.isNullOrEmpty()) {
      LOG.error("Request failed. Status code: $code. Error message: $errorMessage")
    }
    val result = body()
    return parseResponseCode(code, result)
  }

  abstract fun <T> parseResponseCode(code: Int, result: T?): Result<T, AIServiceError>

  protected suspend fun <T> networkCall(call: suspend () -> Result<T, AIServiceError>): Result<T, AIServiceError> =
    try {
      withContext(Dispatchers.IO) {
        call()
      }
    }
    catch (exception: SocketException) {
      AIServiceError.ConnectionError.asErr()
    }

  companion object {
    private val LOG: Logger = Logger.getInstance(AIServiceConnector::class.java)
  }
}