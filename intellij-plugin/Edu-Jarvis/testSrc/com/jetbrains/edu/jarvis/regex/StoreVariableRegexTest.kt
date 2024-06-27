package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class StoreVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.STORE_VARIABLE.regex

  override fun shouldMatch() =
    listOf(
      TestAnswer("store 3 in `foo`", listOf("foo")), // test the `store` verb
      TestAnswer("stored 3 in `foo`", listOf("foo")), // test the `stored` verb

      TestAnswer("store result in `foo`", listOf("foo")), // test the `result` word
      TestAnswer("stored 3321 in `foo`", listOf("foo")), // test the number
      TestAnswer("store \"hello\" in `greeting`", listOf("greeting")), // test the string value
      TestAnswer("store `myVar` in `foo`", listOf("foo")), // test the value wrapped in backticks
      TestAnswer("store true in `foo`", listOf("foo")), // test true value
      TestAnswer("store false in `foo`", listOf("foo")), // test false value

      TestAnswer("store the result in `buzz`", listOf("buzz")), // test the `the` article (1)
      TestAnswer("store a `pear` in `buzz`", listOf("buzz")), // test the `a` article (1)
      TestAnswer("store an `apple` in `buzz`", listOf("buzz")), // test the `an` article (1)
      TestAnswer("store 3 in the `buzz`", listOf("buzz")), // test the `the` article (2)
      TestAnswer("store 3 in a `buzz`", listOf("buzz")), // test the `a` article (2)
      TestAnswer("store 3 in an `apple`", listOf("apple")), // test the `an` article (2)

      TestAnswer("store the value 3 in `buzz`", listOf("buzz")), // test the optional `value` word
      TestAnswer("store 3 in the variable `buzz`", listOf("buzz")), // test the optional `variable` word

      TestAnswer("store `foo` multiplied by 3 in `buzz`", listOf("buzz")), // test arbitrary text (1)
      TestAnswer("store the result divided by `divisor` in `foo`", listOf("foo")), // test arbitrary text (2)

      TestAnswer("SToRe tHe ResUlT iN `foo`", listOf("foo")), // case-insensitive (1)
      TestAnswer("sToRE THE VALUe 3 IN `buzz`", listOf("buzz")), // case-insensitive (2)
      TestAnswer("STOred 3 In `foo`", listOf("foo")), // test case-insensitive (3)
      TestAnswer("sTORED tHE REsUlT DIVideD bY `divisor` IN `foo`", listOf("foo")), // test case-insensitive (4)

      TestAnswer("store     `foo`   multiplied    by  3    in     `buzz`", listOf("buzz")), // test spacing (1)
      TestAnswer("STOred     3      In    `foo`", listOf("foo")), // test spacing (2)
      TestAnswer("store     an  `apple`   in  `buzz`", listOf("buzz")), // test spacing (3)
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
