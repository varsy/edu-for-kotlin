package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.TestGenerator.generateCreateVariable
import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class CreateVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.CREATE_VARIABLE.regex

  override fun shouldMatch() =
    // Generated smoke tests
    List(NUMBER_OF_RUNS) {
      generateCreateVariable(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      TestAnswer("declare `foo`", listOf("foo")), // test the `declare` verb
      TestAnswer("set up `foo`", listOf("foo")), // test the `set up` verb
      TestAnswer("create `bar`", listOf("bar")), // test the `create` verb

      TestAnswer("create the `buzz`", listOf("buzz")), // test the `the` article
      TestAnswer("create a `buzz`", listOf("buzz")), // test the `a` article
      TestAnswer("declare an `apple`", listOf("apple")), // test the `an` article

      TestAnswer("declare the string `buzz`", listOf("buzz")), // test the optional `string` word
      TestAnswer("declare the random string `buzz`", listOf("buzz")), // test the optional `random string` word
      TestAnswer("set up the empty string `myString`", listOf("myString")), // test the optional `empty string` word
      TestAnswer("declare the variable `buzz`", listOf("buzz")), // test the optional `variable` word
      TestAnswer("create the variable called `foo`", listOf("foo")), // test the optional `called` word

      TestAnswer("deClaRe THE STrinG `buzz`", listOf("buzz")), // case-insensitive (1)
      TestAnswer("CREaTe A `buzz`", listOf("buzz")), // case-insensitive (2)
      TestAnswer("DeClArE `foo`", listOf("foo")), // test case-insensitive (3)
      TestAnswer("cREatE thE vArIabLe cAlLed `foo`", listOf("foo")), // test case-insensitive (4)

      TestAnswer("declare   the  string   `buzz`", listOf("buzz")), // test spacing (1)
      TestAnswer("set   up  the empty string      `myString`", listOf("myString")), // test spacing (2)
      TestAnswer("create    the    variable   called   `foo`", listOf("foo")), // test spacing (3)
    )

  override fun shouldNotMatch() =
  listOf(
    "create `foo()`", // cannot create a function
    "run the `test`", // invalid CALL synonym
    "store the `2test2`", // invalid identifier name
    "create `test(a,b,c,)`", // cannot create a function
    "create `foo", // backticks aren't closed
    "declare bar", // no backticks
    "set up the something `foo`", // invalid grammar
  )

  @Test
  fun `test valid create variable sentences`() = runTestShouldMatch()

  @Test
  fun `test invalid create variable sentences`() = runTestShouldNotMatch()

}
