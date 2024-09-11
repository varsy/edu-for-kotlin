package com.jetbrains.edu.learning.cognifire.utils

import com.jetbrains.edu.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls

@NonNls
private const val BUNDLE_NAME = "cognifireTemplateVariables.cognifire"

object CognifireTemplateVariablesProvider : EduPropertiesBundle(BUNDLE_NAME) {
  private var isCognifire = false

  fun setIsCognifireVariable(value: Boolean) {
    isCognifire = value
  }

  fun getIsCognifireVariable(): Boolean = isCognifire

  fun getCognifireDslVersion(): String = valueOrEmpty("cognifireDslVersion")
}