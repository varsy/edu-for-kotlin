package com.jetbrains.edu.jarvis.highlighting.undefinedidentifier

/**
 * Represents a type of error that was found by the [AnnotatorRule].
 */
enum class AnnotatorError(val message: String) {
  NONE(""),
  UNKNOWN_FUNCTION("prompt.annotator.unknown.function.error"),
  WRONG_NUMBER_OF_ARGUMENTS("prompt.annotator.wrong.number.of.args.error"),
  UNKNOWN_VARIABLE("prompt.annotator.unknown.variable.error"),
}
