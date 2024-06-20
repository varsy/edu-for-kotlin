package com.jetbrains.edu.jarvis.errors

import com.jetbrains.edu.jarvis.enums.AnnotatorError

class AnnotatorParametrizedError(val errorType: AnnotatorError, val params: Array<Any>) {
  companion object {
    val NO_ERROR = AnnotatorParametrizedError(AnnotatorError.NONE, emptyArray())
  }
}
