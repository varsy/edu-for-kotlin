package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.TestGenerator.generateIdentifier
import com.jetbrains.edu.jarvis.TestGenerator.generateNamedFunction
import com.jetbrains.edu.jarvis.models.NamedFunction
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_NUMBER_OF_ARGS
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_NUMBER_OF_ARGS
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.learning.EduTestCase

class NamedFunctionRegexTest: RegexTest, EduTestCase() {

  override val regex = NamedFunction.namedFunctionRegex

  override fun shouldMatch() =
    // Generated smoke tests
    List(NUMBER_OF_RUNS) {
      generateNamedFunction(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random(),
        List((MIN_NUMBER_OF_ARGS..MAX_NUMBER_OF_ARGS).random()) {
          generateIdentifier((MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random())
        }
      )
  } + listOf(
    "foo(1, abc, 2)", // multiple parameters
    "bar()", // simple function
    "buzz(\"hi\")", // function with a string parameter
    "_test3(\"string\", 33.13, 123)", // function with complex parameters
  )

  override fun shouldNotMatch(): List<String> =
    listOf(
    "123test()", // invalid identifier name
    "test (a, b, c", // parentheses aren't closed
    "test(a,b,)", // trailing separator
    "test(a b c)", // no separators
    "test(a,,b,c)", // missing argument
    "test(a,b c)", // missing separator
    "test", // no parentheses
    "(test)", // no identifier name
    "(test", // parentheses aren't closed, no identifier name
    "test)", // parentheses aren't closed, no identifier name
    "()", // no identifier name
    "test!(2)" // invalid identifier name
  )

  fun `test valid named functions`() = runTestShouldMatch()
  fun `test invalid named functions`() = runTestShouldNotMatch()

}
