package com.jetbrains.edu.jarvis.models

import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.AND
import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.ARGUMENT_SEPARATOR
import com.jetbrains.edu.jarvis.highlighting.undefinedidentifier.AnnotatorRuleMatch

data class NamedFunction(override val name: String, val numberOfArguments: IntRange) : NamedEntity {

  constructor(target: AnnotatorRuleMatch)
    : this(
      target.identifier.value,
      getNumberOfArguments(target.arguments ?: "")
    )

  fun isCompatibleWith(other: NamedFunction): Boolean = name == other.name && numberOfArguments.first >= other.numberOfArguments.first && numberOfArguments.last <= other.numberOfArguments.last

  companion object {

    /**
     * Calculates the number of arguments from the `arguments` string.
     * Examples:
     * - `arguments = "1, 2 and 3"` -> one separator, one occurrence of the word 'and' -> returns three
     * - `arguments = "1, 2, 3, 4"` -> three separators, no occurrences of the word 'and' -> returns four
     * - `arguments = ""` -> a blank string -> return zero
     */
    fun getNumberOfArguments(arguments: String) = if (arguments.isNotBlank()) {
      val argumentsWithoutQuotation = arguments.replace(QUOTATION_BLOCK, "X")
      val numberOfArguments = SEPARATOR_REGEX.findAll(argumentsWithoutQuotation).count() + 1
      numberOfArguments..numberOfArguments
    }
    else 0..0

    private val SEPARATOR_REGEX = "(?i)(\\s+$AND\\s+|\\s*$ARGUMENT_SEPARATOR\\s*)+".toRegex()
    private val QUOTATION_BLOCK = """'[^']*'|"[^"]*"|`[^`]*`""".toRegex()
  }

}
