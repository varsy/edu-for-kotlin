package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jarvis.ErrorProcessor
import com.jetbrains.edu.jarvis.enums.AnnotatorError
import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable
import com.jetbrains.edu.kotlin.jarvis.utils.ARGUMENT_SEPARATOR
import com.jetbrains.edu.kotlin.jarvis.utils.CLOSE_PARENTHESIS
import com.jetbrains.edu.kotlin.jarvis.utils.OPEN_PARENTHESIS

class KtErrorProcessor(
  private val context: String,
  private val visibleFunctions: Collection<NamedFunction>,
  private val visibleVariables: Collection<NamedVariable>) : ErrorProcessor {

  override fun processNamedFunction(): AnnotatorParametrizedError {
    val namedFunction = context.toNamedFunction()
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

  override fun processNamedVariable(): AnnotatorParametrizedError {
    val namedVariable = context.toNamedVariable()
    return if(namedVariable !in visibleVariables) {
      AnnotatorParametrizedError(
        AnnotatorError.UNKNOWN_VARIABLE,
        arrayOf(namedVariable.name)
      )
    } else AnnotatorParametrizedError.NO_ERROR
  }

  private fun String.toNamedFunction(): NamedFunction {
    val functionName = this.substringBefore(OPEN_PARENTHESIS)
    val parameters = if (OPEN_PARENTHESIS in this) {
      this
        .substringAfter(OPEN_PARENTHESIS)
        .substringBefore(CLOSE_PARENTHESIS)
    }
    else {
      ""
    }
    val numberOfParameters = if (parameters.isNotBlank()) {
      parameters.count { it == ARGUMENT_SEPARATOR } + 1
    }
    else 0
    return NamedFunction(functionName, numberOfParameters)
  }

  private fun String.toNamedVariable(): NamedVariable = NamedVariable(this)
}