package com.jetbrains.edu.ai.error

import com.jetbrains.edu.ai.messages.BUNDLE
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.Err
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

sealed class AIServiceError(@PropertyKey(resourceBundle = BUNDLE) val messageKey: String) {
  data object ConnectionError : AIServiceError("ai.service.could.not.connect")
  data object ServiceUnavailable : AIServiceError("ai.service.is.currently.unavailable")

  fun asErr(): Err<AIServiceError> = Err(this)

  fun message(): @NonNls String = EduAIBundle.message(messageKey)
}