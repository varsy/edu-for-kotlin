package com.jetbrains.edu.cognifire

import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.getCallSynonyms
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.getCreateSynonyms
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.getDataStructureSynonyms
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.getEachSynonyms
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.getElementSynonyms
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.getFunctionSynonyms
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.getLoopSynonyms
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.GrammarRegex.getOverSynonyms
import com.jetbrains.edu.cognifire.regex.TestAnswer

object TestGenerator {
  private const val THE = "the"

  private val UNDERSCORE_RANGE = '_'..'_'

  private const val EMPTY_STRING = ""

  private val letters = ('a'..'z') + ('A'..'Z')
  private val digits = ('0'..'9')

  /**
   * Generates a valid random-named identifier.
   * Example of a generated string: `foo12`.
   */
  private fun generateIdentifier(identifierNameLength: Int) =
    (letters + UNDERSCORE_RANGE).random() +
    (1 until identifierNameLength)
      .map { (letters + digits + UNDERSCORE_RANGE).random() }
      .joinToString(EMPTY_STRING)

  /**
   * Generates a sentence interpreted as a variable creation.
   * Example of a generated string: ``Create an empty string `foo` ``.
   */
  fun generateCreateVariable(variableNameLength: Int): TestAnswer {
    val variableName = generateIdentifier(variableNameLength)
    return TestAnswer(
      "${getCreateSynonyms().random()} ${listOf(EMPTY_STRING, THE).random()} `${variableName}`",
      listOf(variableName)
    )
  }

  /**
   * Generates a sentence interpreted as a function call, without parentheses.
   * Example of a generated string: ``Invoke the function `foo` ``.
   */
  fun generateNoParenthesesFunction(functionNameLength: Int): TestAnswer {
    val functionName = generateIdentifier(functionNameLength)
    return TestAnswer(
      "${getCallSynonyms().random()} ${listOf(EMPTY_STRING, THE).random()}" +
      " ${(listOf(EMPTY_STRING) + getFunctionSynonyms()).random()} `$functionName`",
      listOf(functionName)
    )
  }

  /**
   * Generates a sentence interpreted as a loop expression.
   * Example of a generated string: ``For each `i` in the `array` ``.
   */
  fun generateLoopExpression(nameLength: Int): TestAnswer {
    val identifier = generateIdentifier(nameLength)
    val data = generateIdentifier(nameLength)
    return TestAnswer(
      "${getLoopSynonyms().random()} ${getOverSynonyms().withEmptyStringAndRandom()}" +
      " ${getEachSynonyms().withEmptyStringAndRandom()} ${getElementSynonyms().withEmptyStringAndRandom()}" +
      " `$identifier` in ${listOf(EMPTY_STRING, THE).random()} ${getDataStructureSynonyms().withEmptyStringAndRandom()} `${data}`",
      listOf(identifier, data)
    )
  }

  private fun List<String>.withEmptyStringAndRandom(): String = (this + EMPTY_STRING).random()
}
