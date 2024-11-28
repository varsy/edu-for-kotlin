package com.jetbrains.edu.cognifire.regex

import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class StoreVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.STORE_VARIABLE.regex

  override fun shouldMatch() =
    listOf(
      TestAnswer("store 3 in `foo`", listOf("store", "3", "in", "foo")), // test the `store` verb
      TestAnswer("stored 3 in `foo`", listOf("stored", "3", "in", "foo")), // test the `stored` verb

      TestAnswer("store result in `foo`", listOf("store", "result", "in", "foo")), // test the `result` word
      TestAnswer("stored 3321 in `foo`", listOf("stored", "3321", "in", "foo")), // test the number
      TestAnswer("store \"hello\" in `greeting`", listOf("store", "\"hello\"", "in", "greeting")), // test the string value
      TestAnswer("store `myVar` in `foo`", listOf("store", "`myVar`", "in", "foo")), // test the value wrapped in backticks
      TestAnswer("store true in `foo`", listOf("store", "true", "in", "foo")), // test true value
      TestAnswer("store false in `foo`", listOf("store", "false", "in", "foo")), // test false value

      TestAnswer("store the result in `buzz`", listOf("store", "result", "in", "buzz")), // test the `the` article, before the value
      TestAnswer("store a `pear` in `buzz`", listOf("store", "`pear`", "in", "buzz")), // test the `a` article, before the value
      TestAnswer("store an `apple` in `buzz`", listOf("store", "`apple`", "in", "buzz")), // test the `an` article, before the value
      TestAnswer("store 3 in the `buzz`", listOf("store", "3", "in", "buzz")), // test the `the` article, before the variable name
      TestAnswer("store 3 in a `buzz`", listOf("store", "3", "in", "buzz")), // test the `a` article, before the variable name
      TestAnswer("store 3 in an `apple`", listOf("store", "3", "in", "apple")), // test the `an` article, before the variable name

      TestAnswer("store the value 3 in `buzz`", listOf("store", "3", "in", "buzz")), // test the optional `value` word
      TestAnswer("store 3 in the variable `buzz`", listOf("store", "3", "in", "buzz")), // test the optional `variable` word

      TestAnswer("store `foo` multiplied by 3 in `buzz`", listOf("store", "`foo`", "in", "buzz")), // test arbitrary text
      TestAnswer("store the result divided by `divisor` in `foo`", listOf("store", "result", "in", "foo")), // test arbitrary text

      TestAnswer("SToRe tHe ResUlT iN `foo`", listOf("SToRe", "ResUlT", "iN", "foo")), // test case-insensitive
      TestAnswer("sToRE THE VALUe 3 IN `buzz`", listOf("sToRE", "3", "IN", "buzz")), // test case-insensitive
      TestAnswer("STOred 3 In `foo`", listOf("STOred", "3", "In", "foo")), // test case-insensitive
      TestAnswer("sTORED tHE REsUlT DIVideD bY `divisor` IN `foo`", listOf("sTORED", "REsUlT", "IN", "foo")), // test case-insensitive

      TestAnswer("store     `foo`   multiplied    by  3    in     `buzz`", listOf("store", "`foo`", "in", "buzz")), // test spacing
      TestAnswer("STOred     3      In    `foo`", listOf("STOred", "3", "In", "foo")), // test spacing
      TestAnswer("store     an  `apple`   in  `buzz`", listOf("store", "`apple`", "in", "buzz")), // test spacing
    )

  override fun shouldNotMatch() =
    listOf(
      "store 3 in `foo()`", // cannot store a function
      "storee the `test3`", // typo in the word "storee"
      "store the `test`", // it is not specified what is stored
      "store the `2test2`", // invalid identifier name
      "call `test(a,b,c,)`", // invalid grammar
      "store the knowledge in `foo`", // invalid grammar
    )

  @Test
  fun `test valid store variable sentences`() = runTestShouldMatch()

  @Test
  fun `test invalid store variable sentences`() = runTestShouldNotMatch()

}
