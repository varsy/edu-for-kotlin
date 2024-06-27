package com.jetbrains.edu.jarvis.models

import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.AND
import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.ARGUMENT_SEPARATOR
import com.jetbrains.edu.jarvis.highlighting.AnnotatorRuleMatch

data class NamedFunction(override val name: String, val numberOfArguments: Int) : NamedEntity {

  constructor(target: AnnotatorRuleMatch)
    : this(
      target.identifier.value,
      getNumberOfArguments(target.arguments ?: "")
    )

  companion object {

    /**
     * Calculates the number of arguments from the `arguments` string.
     * Examples:
     * - `arguments = "1, 2 and 3"` -> one separator, presence of the word 'and' -> returns three
     * - `arguments = "1, 2, 3, 4"` -> three separators, no presence of the word 'and' -> returns four
     * - `arguments = ""` -> a blank string -> return zero
     */
    fun getNumberOfArguments(arguments: String) = if (arguments.isNotBlank()) {
      arguments.count { it == ARGUMENT_SEPARATOR } + arguments.containsAndInt() + 1
    }
    else 0

    private fun String.containsAndInt() = if (AND in this.lowercase()) 1 else 0

  }

}
