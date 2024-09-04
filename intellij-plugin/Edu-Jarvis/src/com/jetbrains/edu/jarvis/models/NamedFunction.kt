package com.jetbrains.edu.jarvis.models

import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.AND
import com.jetbrains.edu.jarvis.ErrorProcessor.Companion.ARGUMENT_SEPARATOR
import com.jetbrains.edu.jarvis.highlighting.undefinedidentifier.AnnotatorRuleMatch

data class NamedFunction(override val name: String, var numberOfArguments: IntRange, val arguments: List<String>) : NamedEntity {

  constructor(target: AnnotatorRuleMatch) : this(
    name = target.identifier.value,
    arguments = getArguments(target.arguments),
    numberOfArguments = IntRange.EMPTY
  ) {
    this.numberOfArguments = arguments.let { it.size..it.size }
  }

  fun isCompatibleWith(other: NamedFunction): Boolean =
    name == other.name && numberOfArguments.first >= other.numberOfArguments.first && numberOfArguments.last <= other.numberOfArguments.last

  fun argumentsToString() = arguments.joinToString(ARGUMENT_SEPARATOR.toString()).let { if (it.isBlank()) "" else ": $it" }

  companion object {
    /**
     * Parsing arguments from the `arguments` string.
     * Examples:
     * - `arguments = "1, 2 and 3"` -> one separator, one occurrence of the word 'and' -> returns three
     * - `arguments = "1, 2, 3, 4"` -> three separators, no occurrences of the word 'and' -> returns four
     * - `arguments = ""` -> a blank string -> return zero
     */
    internal fun getArguments(arguments: String?): List<String> {
      if (arguments.isNullOrBlank()) return emptyList()
      val quotationRanges = QUOTATION_BLOCK.findAll(arguments).map { it.range }
      val separatorRanges = listOf(IntRange(-1, -1)) + SEPARATOR_REGEX.findAll(arguments).map { it.range }
          .filterNot { separatorRange ->
            quotationRanges.any { it.contains(separatorRange.first) && it.contains(separatorRange.last) }
          }
      return separatorRanges.mapIndexed { index, range ->
        arguments.substring(range.last + 1, separatorRanges.getOrNull(index + 1)?.first ?: arguments.length).trim()
      }.filter(String::isNotEmpty)
    }

    private val SEPARATOR_REGEX = "(?i)(\\s+$AND\\s+|$ARGUMENT_SEPARATOR)+".toRegex()
    private val QUOTATION_BLOCK = """'[^']*'|"[^"]*"|`[^`]`""".toRegex()
  }
}
