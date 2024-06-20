package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jarvis.ErrorProcessor
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator
import com.jetbrains.edu.jarvis.enums.AnnotatorError
import com.jetbrains.edu.jarvis.errors.AnnotatorParametrizedError
import com.jetbrains.edu.jarvis.models.FunctionCall
import com.jetbrains.edu.kotlin.jarvis.utils.ARGUMENT_SEPARATOR
import com.jetbrains.edu.kotlin.jarvis.utils.CLOSE_PARENTHESIS
import com.jetbrains.edu.kotlin.jarvis.utils.OPEN_PARENTHESIS

class KtErrorProcessor(private val context: String, private val visibleFunctions: Collection<FunctionCall>) : ErrorProcessor {
  override fun processIsolatedCode(): AnnotatorParametrizedError {
    return when {
      !context.isAFunctionCall() -> AnnotatorParametrizedError.NO_ERROR
      else -> {
        val functionCall = context.toFunctionCall()
        when {
          visibleFunctions.none { it.name == functionCall.name } ->
            AnnotatorParametrizedError(
              AnnotatorError.UNKNOWN_FUNCTION,
              arrayOf(functionCall.name)
            )
          functionCall !in visibleFunctions ->
            AnnotatorParametrizedError(
              AnnotatorError.WRONG_NUMBER_OF_ARGUMENTS,
              arrayOf(functionCall.name, functionCall.numberOfArguments)
            )
          else -> AnnotatorParametrizedError.NO_ERROR
        }
      }
    }
  }

  override fun processNoParenthesesFunctionCall(): AnnotatorParametrizedError {
    val functionCall = context.toFunctionCall()
    return when {
      visibleFunctions.none { it.name == functionCall.name } ->
        AnnotatorParametrizedError(
          AnnotatorError.UNKNOWN_FUNCTION,
          arrayOf(functionCall.name)
        )
      functionCall !in visibleFunctions ->
        AnnotatorParametrizedError(
          AnnotatorError.WRONG_NUMBER_OF_ARGUMENTS,
          arrayOf(functionCall.name, functionCall.numberOfArguments)
        )
      else -> AnnotatorParametrizedError.NO_ERROR
    }
  }

  private fun String.toFunctionCall(): FunctionCall {
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
    return FunctionCall(functionName, numberOfParameters)
  }

  private fun String.isAFunctionCall() =
    DescriptionErrorAnnotator.functionCallRegex.matches(this)
}