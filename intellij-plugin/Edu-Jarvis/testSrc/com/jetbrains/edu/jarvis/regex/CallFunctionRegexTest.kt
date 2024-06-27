package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.TestGenerator.generateNoParenthesesFunction
import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class CallFunctionRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.CALL_FUNCTION.regex

  override fun shouldMatch() =
    // generated smoke tests
    List(NUMBER_OF_RUNS) {
      generateNoParenthesesFunction(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      TestAnswer("call the `foo`", listOf("foo")), // test the `call` verb
      TestAnswer("invoke `bar`", listOf("bar")), // test the `invoke` verb
      TestAnswer("run `bar`", listOf("bar")), // test the `run` verb
      TestAnswer("execute `buzz`", listOf("buzz")), // test the `execute` verb

      TestAnswer("call the `buzz`", listOf("buzz")), // test the `the` article
      TestAnswer("call a `buzz`", listOf("buzz")), // test the `a` article
      TestAnswer("invoke an `apple`", listOf("apple")), // test the `an` article

      TestAnswer("call the function `buzz`", listOf("buzz")), // test the optional `function` word

      TestAnswer("call the function `foo` with 2", listOf("foo", " with 2")), // test single argument capturing
      TestAnswer("call the function `foo` with 2, 3, 4", listOf("foo", " with 2, 3, 4")), // test multiple arguments capturing
      TestAnswer("call `buzz` with 2 and 1", listOf("buzz", " with 2 and 1")), // test the `and` word
      TestAnswer(
        "call the function `foo` with `bar`, `myVar` and `buzz`",
        listOf("foo", " with `bar`, `myVar` and `buzz`")
      ), // test arguments wrapped in backticks
      TestAnswer(
        "call the function `sayHello` with \"hello\", \"to\", \"everyone\"",
        listOf("sayHello", " with \"hello\", \"to\", \"everyone\"")
      ), // test string arguments

      TestAnswer("CaLl `print`", listOf("print")), // case-insensitive (1)
      TestAnswer("InvOKe A `buzz`", listOf("buzz")), // case-insensitive (2)
      TestAnswer("rUn `buzz` wItH 2 And 1", listOf("buzz", " wItH 2 And 1")), // test case-insensitive (3)
      TestAnswer("exEcUTe `foo` witH 2, 3 aNd 1", listOf("foo", " witH 2, 3 aNd 1")), // test case-insensitive (4)

      TestAnswer("call  the    function `foo`    with 1, 2, 3", listOf("foo", "    with 1, 2, 3")), // test spacing (1)
      TestAnswer("call   `buzz`   with       2  and 1", listOf("buzz", "   with       2  and 1")), // test spacing (2)
      TestAnswer("invoke  an    `apple`", listOf("apple")), // test spacing (3)

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

  @Test
  fun `test valid function call sentences`() = runTestShouldMatch()

  @Test
  fun `test invalid function call sentences`() = runTestShouldNotMatch()

}
