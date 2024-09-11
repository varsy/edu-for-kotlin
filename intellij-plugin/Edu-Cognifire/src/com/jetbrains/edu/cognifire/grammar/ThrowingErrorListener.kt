package com.jetbrains.edu.cognifire.grammar

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

class ThrowingErrorListener : BaseErrorListener() {

  companion object {
    val INSTANCE: ThrowingErrorListener = ThrowingErrorListener()
  }

  override fun syntaxError(
    recognizer: Recognizer<*, *>?,
    offendingSymbol: Any?,
    line: Int,
    charPositionInLine: Int,
    msg: String?,
    e: RecognitionException?
  ) {
    throw AssertionError("Failed to parse at line $line position $charPositionInLine due to $msg", e)
  }
}
