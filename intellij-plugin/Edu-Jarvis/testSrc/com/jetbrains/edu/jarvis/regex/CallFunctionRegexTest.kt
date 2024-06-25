package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.TestGenerator.generateNoParenthesesFunction
import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.learning.EduTestCase

class CallFunctionRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.CALL_FUNCTION.regex
  override fun shouldMatch(): List<String> = emptyList()

  override fun shouldMatchGroup() =
    List(NUMBER_OF_RUNS) {
      generateNoParenthesesFunction(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      // Test all CALL synonyms
      TestAnswer("InVOke `bar`", listOf("bar")),
      TestAnswer("run the `buzz`", listOf("buzz")),
      TestAnswer("call `print`", listOf("print")),
      TestAnswer("execute ThE function `welcome`", listOf("welcome")),
      TestAnswer("run the `calculate`", listOf("calculate")),
      TestAnswer("ExECUte `display`", listOf("display")),
      TestAnswer("calls the `exit`", listOf("exit")),
      TestAnswer("Invokes the Function `start`", listOf("start")),
      TestAnswer("Run `stop`", listOf("stop")),
      TestAnswer("ruN THE  `foo`", listOf("foo")),
      // Test argument capturing
      TestAnswer("call the function `foo` with 2 and 1",listOf("foo", " with 2 and 1")),
      TestAnswer("calls the `exit` with `error`, `errorCode`", listOf("exit", " with `error`, `errorCode`")),
      TestAnswer("call the function `foo` with 1, 2, 3", listOf("foo", " with 1, 2, 3")),

      )

  override fun shouldNotMatch() =
    listOf(
    "call `123test`", // invalid identifier name
    "run the `test (a, b, c`", // parentheses aren't closed
    "run the `test(a,,b,c)`", // missing argument
    "execute `test(a,b c)`", // missing separator
    "call `test(a,b,c,)`", // trailing separator
    "print `foo`", // wrong grammar
    "run bar", // no backticks
    "invoke the `foo", // no closing backticks
  )


  fun `test valid function call sentences`() = runTestShouldMatchGroup()
  fun `test invalid function call sentences`() = runTestShouldNotMatch()

}