package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.enums.AnnotatorRule
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.generateNoParenthesesFunction
import com.jetbrains.edu.learning.EduTestCase

class NoParenthesesFunctionRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.NO_PARENTHESES_FUNCTION.regex
  override fun shouldMatch(): List<String> = emptyList()

  override fun shouldMatchGroup() =
    List(NUMBER_OF_RUNS) {
      generateNoParenthesesFunction(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      TestAnswer("call the function `foo`","foo"),
      TestAnswer("InVOke `bar`", "bar"),
      TestAnswer("run the `buzz`", "buzz"),
      TestAnswer("call `print`","print"),
      TestAnswer("execute ThE function `welcome`","welcome"),
      TestAnswer("run the `calculate`","calculate"),
      TestAnswer("ExECUte `display`","display"),
      TestAnswer("call the `exit`","exit"),
      TestAnswer("Invoke the Function `start`","start"),
      TestAnswer("Run `stop`","stop"),
      TestAnswer("ruN THE  `foo`","foo"),
    )

  override fun shouldNotMatch() =
    listOf(
    "call `123test`",
    "run the `test (a, b, c`",
    "run the `test(a,,b,c)`",
    "execute `test(a,b c)`",
    "call `test(a,b,c,)`",
    "print `foo`",
    "run bar",
    "invoke the foo",
  )


  fun testValidNoParenthesesFunction() = testShouldMatchGroup()
  fun testInvalidNoParenthesesFunction() = testShouldNotMatch()

}