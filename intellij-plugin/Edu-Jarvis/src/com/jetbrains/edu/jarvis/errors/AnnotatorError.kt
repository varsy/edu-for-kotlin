package com.jetbrains.edu.jarvis.errors

enum class AnnotatorError(val message: String) {
  NONE(""),
  UNKNOWN_FUNCTION("description.annotator.unknown.function.error"),
  WRONG_NUMBER_OF_ARGUMENTS("description.annotator.wrong.number.of.args.error"),
}
