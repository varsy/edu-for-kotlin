package com.jetbrains.edu.learning.jarvis.utils

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls

@NonNls
private const val BUNDLE_NAME = "jarvisTemplateVariables.jarvis"

object JarvisTemplateVariablesProvider : EduPropertiesBundle(BUNDLE_NAME) {
  private var isJarvis = false

  fun setIsJarvisVariable(value: Boolean) {
    isJarvis = value
  }

  fun getIsJarvisVariable(): Boolean = isJarvis

  fun getJarvisDslVersion(): String = valueOrEmpty("jarvisDslVersion")
}
