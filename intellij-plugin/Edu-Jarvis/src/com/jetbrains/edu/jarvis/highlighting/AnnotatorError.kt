package com.jetbrains.edu.jarvis.highlighting

/**
 * Represents a type of error that was found by the [AnnotatorRule].
 */
enum class AnnotatorError(val message: String) {
  NONE(""),
  UNKNOWN_FUNCTION("description.annotator.unknown.function.error"),
  WRONG_NUMBER_OF_ARGUMENTS("description.annotator.wrong.number.of.args.error"),
  UNKNOWN_VARIABLE("description.annotator.unknown.variable.error"),
}
