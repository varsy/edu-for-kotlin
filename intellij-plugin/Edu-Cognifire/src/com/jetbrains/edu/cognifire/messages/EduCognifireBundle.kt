package com.jetbrains.edu.cognifire.messages

import com.jetbrains.edu.learning.messages.EduBundle
import org.jetbrains.annotations.PropertyKey
import org.jetbrains.annotations.NonNls

@NonNls
private const val BUNDLE = "messages.EduCognifireBundle"

object EduCognifireBundle : EduBundle(BUNDLE) {
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }
}
