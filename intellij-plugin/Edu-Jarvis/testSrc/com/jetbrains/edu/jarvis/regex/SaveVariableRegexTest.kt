package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class SaveVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.SAVE_VARIABLE.regex

  override fun shouldMatch() =
    listOf(
      TestAnswer("save 3 to `foo`", listOf("foo")), // test the `save` verb

      TestAnswer("save result to `foo`", listOf("foo")), // test the `result` word
      TestAnswer("save 3321 to `foo`", listOf("foo")), // test the number
      TestAnswer("save \"hello\" to `greeting`", listOf("greeting")), // test the string value
      TestAnswer("save `myVar` to `foo`", listOf("foo")), // test the value wrapped in backticks
      TestAnswer("save true to `foo`", listOf("foo")), // test true value
      TestAnswer("save false to `foo`", listOf("foo")), // test false value

      TestAnswer("save the result to `buzz`", listOf("buzz")), // test the `the` article (1)
      TestAnswer("save a `pear` to `buzz`", listOf("buzz")), // test the `a` article (1)
      TestAnswer("save an `apple` to `buzz`", listOf("buzz")), // test the `an` article (1)
      TestAnswer("save 3 to the `buzz`", listOf("buzz")), // test the `the` article (2)
      TestAnswer("save 3 to a `buzz`", listOf("buzz")), // test the `a` article (2)
      TestAnswer("save 3 to an `apple`", listOf("apple")), // test the `an` article (2)

      TestAnswer("save the value 3 to `buzz`", listOf("buzz")), // test the optional `value` word
      TestAnswer("save 3 to the variable `buzz`", listOf("buzz")), // test the optional `variable` word

      TestAnswer("save `foo` incremented by 3 to `buzz`", listOf("buzz")), // test arbitrary text (1)
      TestAnswer("save the result multiplied by `multiplier` to `foo`", listOf("foo")), // test arbitrary text (2)

      TestAnswer("sAvE tHe ResUlT tO `foo`", listOf("foo")), // case-insensitive (1)
      TestAnswer("SAVE THE VALUe 3 To `buzz`", listOf("buzz")), // case-insensitive (2)
      TestAnswer("SaVE 3 tO `foo`", listOf("foo")), // test case-insensitive (3)
      TestAnswer("saVe tHE REsUlT DIVideD bY `divisor` TO `foo`", listOf("foo")), // test case-insensitive (4)

      TestAnswer("save     `foo`   multiplied    by  3    to    `buzz`", listOf("buzz")), // test spacing (1)
      TestAnswer("Save     3      tO    `foo`", listOf("foo")), // test spacing (2)
      TestAnswer("save     an  `apple`   to  `buzz`", listOf("buzz")), // test spacing (3)
    )

  override fun shouldNotMatch() =
    listOf(
      "save 3 in `foo()`", // cannot save a function
      "savee the `test3`", // typo in the word "savee"
      "save the `test`", // it is not specified what is saved
      "save the `2test2`", // invalid identifier name
      "call `test(a,b,c,)`", // invalid grammar
      "save the knowledge in `foo`", // invalid grammar
    )

  @Test
  fun `test valid save variable sentences`() = runTestShouldMatch()

  @Test
  fun `test invalid save variable sentences`() = runTestShouldNotMatch()

}
