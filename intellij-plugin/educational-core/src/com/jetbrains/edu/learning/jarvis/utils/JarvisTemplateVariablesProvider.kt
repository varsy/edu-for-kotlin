package com.jetbrains.edu.learning.jarvis.utils

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_NAME = "jarvisTemplateVariables.jarvis"

object JarvisTemplateVariablesProvider : EduPropertiesBundle(BUNDLE_NAME) {
  private fun value(@Suppress("SameParameterValue") @PropertyKey(resourceBundle = BUNDLE_NAME) key: String): String =
    valueOrEmpty(key)

  fun getIsJarvisVariable(): Boolean = value("isJarvis").toBoolean()

  fun getJarvisDslVersion(): String = value("jarvisDslVersion")
}
