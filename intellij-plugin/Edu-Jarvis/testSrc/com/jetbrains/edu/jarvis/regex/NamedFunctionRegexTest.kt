package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.namedFunctionRegex
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_NUMBER_OF_ARGS
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_NUMBER_OF_ARGS
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.generateIdentifier
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.generateNamedFunction
import com.jetbrains.edu.learning.EduTestCase

class NamedFunctionRegexTest: RegexTest, EduTestCase() {

  override val regex = namedFunctionRegex


  override fun shouldMatchGroup(): List<TestAnswer> = emptyList()

  override fun shouldMatch() =
    List(NUMBER_OF_RUNS) {
      generateNamedFunction(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random(),
        List((MIN_NUMBER_OF_ARGS..MAX_NUMBER_OF_ARGS).random()) {
          generateIdentifier((MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random())
        }
      )
  } + listOf(
    "foo(1, abc, 2)",
    "bar()",
    "buzz(\"hi\")",
    "test1(a1,b1,c1)",
    "test2(a2)",
    "_test3(\"string\", variable, 123)",
  )

  override fun shouldNotMatch(): List<String> =
    listOf(
    "123test()",
    "test (a, b, c",
    "test(a,b,)",
    "test(a b c)",
    "test(a,,b,c)",
    "test(a,b c)",
    "test",
    "(test)",
    "(test",
    "test)",
    "()",
    "test!(2)"
  )

  fun testValidNamedFunctions() = testShouldMatch()
  fun testInvalidNamedFunctions() = testShouldNotMatch()

}
