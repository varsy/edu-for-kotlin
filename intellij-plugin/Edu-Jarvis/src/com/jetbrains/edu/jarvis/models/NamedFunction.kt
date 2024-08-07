package com.jetbrains.edu.jarvis.models

import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.AND
import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.ARGUMENT_SEPARATOR
import com.jetbrains.edu.jarvis.highlighting.undefinedidentifier.AnnotatorRuleMatch

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
     * - `arguments = "1, 2 and 3"` -> one separator, one occurrence of the word 'and' -> returns three
     * - `arguments = "1, 2, 3, 4"` -> three separators, no occurrences of the word 'and' -> returns four
     * - `arguments = ""` -> a blank string -> return zero
     */
    fun getNumberOfArguments(arguments: String) = if (arguments.isNotBlank()) {
      arguments.count { it == ARGUMENT_SEPARATOR } + arguments.countAnd() + 1
    }
    else 0

    private fun String.countAnd() = (
      AND_REGEX.findAll(this).count()
      - AND_STRING_LITERAL.findAll(this).count()
      - AND_IDENTIFIER.findAll(this).count()
      )

    private val AND_REGEX = "(?i)$AND".toRegex()
    private val AND_STRING_LITERAL = "(?i)\"[^\"]*$AND[^\"]*\"".toRegex()
    private val AND_IDENTIFIER = "(?i)`[^`]*$AND[^`]*`".toRegex()
  }

}
