package com.jetbrains.edu.learning.eduAssistant.log

import com.intellij.idea.LoggerFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.lazyPub

object Logger {
  internal val default: Logger = LoggerFactory().getLoggerInstance("edu-assistant")

  val eduAssistantLogger: Logger by lazyPub {
    EduAssistantLoggerFactory.getLoggerInstanceOrNull() ?: default
  }
}
