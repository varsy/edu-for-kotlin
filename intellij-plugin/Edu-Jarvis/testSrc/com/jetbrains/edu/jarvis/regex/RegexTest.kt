package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.callSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.declareSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.functionSynonyms
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

interface RegexTest {

  val regex: Regex

  fun shouldMatch(): List<String>

  fun shouldNotMatch(): List<String>

  fun shouldMatchGroup(): List<TestAnswer>

  fun testShouldMatch() =
    shouldMatch().forEach { assertTrue(regex.matches(it)) }

  fun testShouldNotMatch() =
    shouldNotMatch().forEach { assertFalse(regex.matches(it)) }

  fun testShouldMatchGroup() =
    shouldMatchGroup().forEach { assertTrue(regex.find(it.input)!!.groups[1]!!.value == it.answer) }

  companion object {

    const val NUMBER_OF_RUNS = 10
    const val MIN_IDENTIFIER_NAME_LENGTH = 4
    const val MAX_IDENTIFIER_NAME_LENGTH = 30

    const val MIN_NUMBER_OF_ARGS = 0
    const val MAX_NUMBER_OF_ARGS = 10

    private const val EMPTY_STRING = ""
    private const val THE = "the"
    private const val OPEN_PARENTHESIS = "("
    private const val CLOSED_PARENTHESIS = ")"
    private const val ARGUMENT_SEPARATOR = ","

    private val identifierFirstCharacter = ('a'..'z') + ('A'..'Z') + '_'
    private val identifierCharacter = identifierFirstCharacter + ('0'..'9')

    /**
     * Generates a valid random-named function with arguments.
     * Example of a generated string: `foo(arg1, arg2)`.
     */
    fun generateNamedFunction(functionNameLength: Int, arguments: Collection<String>) =
      "${generateIdentifier(functionNameLength)}$OPEN_PARENTHESIS${arguments.joinToString(ARGUMENT_SEPARATOR)}$CLOSED_PARENTHESIS"


    /**
     * Generates a valid random-named identifier.
     * Example of a generated string: `foo12`.
     */
    fun generateIdentifier(identifierNameLength: Int) =
      identifierFirstCharacter.random() +
      (1 until identifierNameLength).map { identifierCharacter.random() }.joinToString(EMPTY_STRING)

    /**
     * Generates a sentence interpreted as a function call, without parentheses.
     * Example of a generated string: ``Invoke the function `foo` ``.
     */
    fun generateNoParenthesesFunction(functionNameLength: Int): TestAnswer {
      val functionName = generateIdentifier(functionNameLength)
      return TestAnswer(
        "${callSynonyms().random()} ${listOf(EMPTY_STRING, THE).random()}" +
        " ${(listOf(EMPTY_STRING) + functionSynonyms()).random()} `$functionName`",
        functionName
      )
    }

    /**
     * Generates a sentence interpreted as a variable declaration.
     * Example of a generated string: ``Create the variable `foo12` ``.
     */
    fun generateVariableDeclaration(variableNameLength: Int): TestAnswer {
      val variableName = generateIdentifier(variableNameLength)
      return TestAnswer(
        "${declareSynonyms().random()} ${listOf(EMPTY_STRING, THE).random()} `${variableName}`",
        variableName
      )
    }
  }
}
