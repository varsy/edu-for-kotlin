package com.jetbrains.edu.aiHints.core.log

import com.intellij.idea.LoggerFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.lazyPub

object Logger {
  private val default: Logger = LoggerFactory().getLoggerInstance("ai-hints")

  val aiHintsLogger: Logger by lazyPub {
    AiHintsLoggerFactory.getLoggerInstanceOrNull() ?: default
  }
}
