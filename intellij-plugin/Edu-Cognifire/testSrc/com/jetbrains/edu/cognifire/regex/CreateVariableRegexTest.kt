package com.jetbrains.edu.cognifire.regex

import com.jetbrains.edu.cognifire.TestGenerator.generateCreateVariable
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRule
import com.jetbrains.edu.cognifire.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.cognifire.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.cognifire.regex.RegexTest.Companion.NUMBER_OF_RUNS
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
      TestAnswer("declare `foo`", listOf("declare", "foo")), // test the `declare` verb
      TestAnswer("initialize `foo`", listOf("initialize", "foo")), // test the `set up` verb
      TestAnswer("create `bar`", listOf("create", "bar")), // test the `create` verb

      TestAnswer("create the `buzz`", listOf("create", "buzz")), // test the `the` article
      TestAnswer("create a `buzz`", listOf("create", "buzz")), // test the `a` article
      TestAnswer("declare an `apple`", listOf("declare", "apple")), // test the `an` article

      TestAnswer("declare the string `buzz`", listOf("declare", "string", "buzz")), // test the optional `string` word
      TestAnswer("declare the random string `buzz`", listOf("declare", "random string", "buzz")), // test the optional `random string` word
      TestAnswer("initialize the empty string `myString`", listOf("initialize", "empty string", "myString")), // test the optional `empty string` word
      TestAnswer("declare the variable `buzz`", listOf("declare", "buzz")), // test the optional `variable` word
      TestAnswer("create the variable called `foo`", listOf("create", "foo")), // test the optional `called` word

      TestAnswer("deClaRe THE STrinG `buzz`", listOf("deClaRe", "STrinG", "buzz")), // test case-insensitive
      TestAnswer("CREaTe A `buzz`", listOf("CREaTe", "buzz")), // test case-insensitive
      TestAnswer("DeClArE `foo`", listOf("DeClArE", "foo")), // test case-insensitive
      TestAnswer("cREatE thE vArIabLe cAlLed `foo`", listOf("cREatE", "foo")), // test case-insensitive

      TestAnswer("declare   the  string   `buzz`", listOf("declare", "string", "buzz")), // test spacing
      TestAnswer("initialize   the empty string      `myString`", listOf("initialize", "empty string", "myString")), // test spacing
      TestAnswer("create    the    variable   called   `foo`", listOf("create", "foo")), // test spacing
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
