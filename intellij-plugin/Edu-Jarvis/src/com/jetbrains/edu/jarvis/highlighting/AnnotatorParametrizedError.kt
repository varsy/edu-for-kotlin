package com.jetbrains.edu.jarvis.highlighting

/**
 * A parametrized version of [AnnotatorError],
 * designed to hold information about a specific instance of [AnnotatorError], along with associated parameters.
 * These parameters are further used to display the error message.
 */
class AnnotatorParametrizedError(val errorType: AnnotatorError, val params: Array<Any>) {
  companion object {
    val NO_ERROR = AnnotatorParametrizedError(AnnotatorError.NONE, emptyArray())
  }
}
