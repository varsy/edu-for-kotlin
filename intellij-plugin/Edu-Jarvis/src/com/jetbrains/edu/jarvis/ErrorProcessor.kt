package com.jetbrains.edu.jarvis

import com.jetbrains.edu.jarvis.highlighting.AnnotatorError
import com.jetbrains.edu.jarvis.highlighting.AnnotatorParametrizedError
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable

/**
 * Returns an [AnnotatorParametrizedError] associated with the relevant context.
 */
interface ErrorProcessor {

  val visibleFunctions: MutableCollection<NamedFunction>
  val visibleVariables: MutableCollection<NamedVariable>

  /**
   * Processes the provided `target` string as a named function and
   * returns the associated [AnnotatorParametrizedError].
   */
  fun processNamedFunction(target: String, arguments: String? = null): AnnotatorParametrizedError {
    val namedFunction = target.toNamedFunction(arguments)
    return when {
      visibleFunctions.none { it.name == namedFunction.name } ->
        AnnotatorParametrizedError(
          AnnotatorError.UNKNOWN_FUNCTION,
          arrayOf(namedFunction.name)
        )

      namedFunction !in visibleFunctions ->
        AnnotatorParametrizedError(
          AnnotatorError.WRONG_NUMBER_OF_ARGUMENTS,
          arrayOf(namedFunction.name, namedFunction.numberOfArguments)
        )

      else -> AnnotatorParametrizedError.NO_ERROR
    }
  }

  /**
   * Processes the provided `target` as a named variable and
   * returns the associated [AnnotatorParametrizedError].
   */
  fun processNamedVariable(target: String): AnnotatorParametrizedError {
    val namedVariable = target.toNamedVariable()
    return if (namedVariable !in visibleVariables) {
      AnnotatorParametrizedError(
        AnnotatorError.UNKNOWN_VARIABLE,
        arrayOf(namedVariable.name)
      )
    }
    else AnnotatorParametrizedError.NO_ERROR
  }

  /**
   * Casts a [String] to a [NamedFunction]
   */
  fun String.toNamedFunction(arguments: String? = null): NamedFunction

  /**
   * Casts a [String] to a [NamedVariable]
   */
  fun String.toNamedVariable(): NamedVariable

  companion object {
    const val AND = "and"
  }

}
