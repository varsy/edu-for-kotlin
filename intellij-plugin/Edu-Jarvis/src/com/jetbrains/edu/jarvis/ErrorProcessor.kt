package com.jetbrains.edu.jarvis

import com.jetbrains.edu.jarvis.highlighting.AnnotatorError
import com.jetbrains.edu.jarvis.highlighting.AnnotatorParametrizedError
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable

/**
 * Returns an [AnnotatorParametrizedError] associated with the relevant context.
 */
class ErrorProcessor(
  private val visibleFunctions: Collection<NamedFunction>, val visibleVariables: MutableCollection<NamedVariable>
) {

  /**
   * Processes the provided [NamedFunction].
   * Returns the associated [AnnotatorParametrizedError].
   */
  fun processNamedFunction(namedFunction: NamedFunction): AnnotatorParametrizedError {

    return when {
      visibleFunctions.none { it.name == namedFunction.name } -> AnnotatorParametrizedError(
        AnnotatorError.UNKNOWN_FUNCTION, arrayOf(namedFunction.name)
      )

      namedFunction !in visibleFunctions -> AnnotatorParametrizedError(
        AnnotatorError.WRONG_NUMBER_OF_ARGUMENTS, arrayOf(namedFunction.name, namedFunction.numberOfArguments)
      )

      else -> AnnotatorParametrizedError.NO_ERROR

    }
  }

  /**
   * Processes the provided [NamedVariable].
   * Returns the associated [AnnotatorParametrizedError].
   */
  fun processNamedVariable(namedVariable: NamedVariable): AnnotatorParametrizedError {
    return if (namedVariable !in visibleVariables) {
      AnnotatorParametrizedError(
        AnnotatorError.UNKNOWN_VARIABLE, arrayOf(namedVariable.name)
      )
    }
    else AnnotatorParametrizedError.NO_ERROR
  }

  companion object {
    const val AND = "and"
    const val ARGUMENT_SEPARATOR = ','
  }

}
