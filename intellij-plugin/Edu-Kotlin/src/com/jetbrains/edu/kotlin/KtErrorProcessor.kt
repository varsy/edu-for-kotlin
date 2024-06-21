package com.jetbrains.edu.kotlin

import com.jetbrains.edu.jarvis.ErrorProcessor
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.models.NamedVariable
import com.jetbrains.edu.kotlin.jarvis.utils.ARGUMENT_SEPARATOR
import com.jetbrains.edu.kotlin.jarvis.utils.CLOSE_PARENTHESIS
import com.jetbrains.edu.kotlin.jarvis.utils.EMPTY_STRING
import com.jetbrains.edu.kotlin.jarvis.utils.OPEN_PARENTHESIS

class KtErrorProcessor(
  override val visibleFunctions: MutableSet<NamedFunction>,
  override val visibleVariables: MutableSet<NamedVariable>
) : ErrorProcessor {

  override fun String.toNamedFunction(): NamedFunction {
    val functionName = this.substringBefore(OPEN_PARENTHESIS)
    val parameters = if (OPEN_PARENTHESIS in this) {
      this.substringAfter(OPEN_PARENTHESIS).substringBefore(CLOSE_PARENTHESIS)
    }
    else EMPTY_STRING

    val numberOfParameters = getNumberOfParameters(parameters)
    return NamedFunction(functionName, numberOfParameters)
  }

  private fun getNumberOfParameters(parameters: String) = if (parameters.isNotBlank()) {
    parameters.count { it == ARGUMENT_SEPARATOR } + 1
  }
  else 0

  override fun String.toNamedVariable(): NamedVariable = NamedVariable(this)
}