package com.jetbrains.edu.jarvis.models

import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.AND
import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.ARGUMENT_SEPARATOR
import com.jetbrains.edu.jarvis.highlighting.AnnotatorRuleMatch

data class NamedFunction(override val name: String, val numberOfArguments: Int) : NamedEntity {

  constructor(target: AnnotatorRuleMatch)
    : this(
      target.identifier.value,
      getNumberOfParameters(target.arguments ?: "")
    )

  companion object {

    fun getNumberOfParameters(parameters: String) = if (parameters.isNotBlank()) {
      parameters.count { it == ARGUMENT_SEPARATOR } + parameters.containsAndInt() + 1
    }
    else 0

    private fun String.containsAndInt() = if (AND in this.lowercase()) 1 else 0

  }

}
