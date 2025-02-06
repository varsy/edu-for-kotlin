package com.jetbrains.edu.ai.clippy.assistant.messages

import com.jetbrains.edu.learning.messages.EduBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
const val BUNDLE: String = "messages.EduAIClippyAssistantBundle"

object EduAIClippyAssistantBundle : EduBundle(BUNDLE) {
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }
}