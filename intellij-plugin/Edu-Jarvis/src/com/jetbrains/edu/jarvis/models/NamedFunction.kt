package com.jetbrains.edu.jarvis.models

import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.AND
import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.ARGUMENT_SEPARATOR
import com.jetbrains.edu.jarvis.highlighting.undefinedidentifier.AnnotatorRuleMatch

data class NamedFunction(override val name: String, val numberOfArguments: IntRange, val arguments: List<String>? = null) : NamedEntity {

  constructor(target: AnnotatorRuleMatch)
    : this(
      target.identifier.value,
      getNumberOfArguments(target.arguments ?: ""),
      target.arguments?.let { getArgumentsList(it) }
    )

  fun isCompatibleWith(other: NamedFunction): Boolean = name == other.name && numberOfArguments.first >= other.numberOfArguments.first && numberOfArguments.last <= other.numberOfArguments.last

  companion object {
    fun getArgumentsList(arguments: String): List<String> = if (arguments.isNotBlank()) {
      val quotationRanges = QUOTATION_BLOCK.findAll(arguments).map { it.range }.toList()
      val separatorRanges = listOf(IntRange(-1, -1)) + SEPARATOR_REGEX.findAll(arguments).map { it.range }.toList().filter { separatorRange ->
        quotationRanges.none { it.contains(separatorRange.first) && it.contains(separatorRange.last) }
      }
      separatorRanges.mapIndexed { index, range ->
        arguments.substring(range.last + 1, separatorRanges.getOrNull(index + 1)?.first ?: arguments.length)
      }.map(String::trim).filter(String::isNotEmpty)
    } else emptyList()

    /**
     * Calculates the number of arguments from the `arguments` string.
     * Examples:
     * - `arguments = "1, 2 and 3"` -> one separator, one occurrence of the word 'and' -> returns three
     * - `arguments = "1, 2, 3, 4"` -> three separators, no occurrences of the word 'and' -> returns four
     * - `arguments = ""` -> a blank string -> return zero
     */
    fun getNumberOfArguments(arguments: String): IntRange {
      val numberOfArguments = getArgumentsList(arguments).size
      return numberOfArguments..numberOfArguments
    }

    private val SEPARATOR_REGEX = "(?i)(\\s+$AND\\s+|$ARGUMENT_SEPARATOR)+".toRegex()
    private val QUOTATION_BLOCK = """'[^']*'|"[^"]*"|`[^`]`""".toRegex()
  }

}
