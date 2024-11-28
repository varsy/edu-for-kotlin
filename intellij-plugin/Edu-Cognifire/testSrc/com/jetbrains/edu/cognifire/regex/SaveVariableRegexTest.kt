package com.jetbrains.edu.cognifire.regex

import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class SaveVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.SAVE_VARIABLE.regex

  override fun shouldMatch() =
    listOf(
      TestAnswer("save 3 to `foo`", listOf("save", "3", "to", "foo")), // test the `save` verb

      TestAnswer("save result to `foo`", listOf("save", "result", "to", "foo")), // test the `result` word
      TestAnswer("save 3321 to `foo`", listOf("save", "3321", "to", "foo")), // test the number
      TestAnswer("save \"hello\" to `greeting`", listOf("save", "\"hello\"", "to", "greeting")), // test the string value
      TestAnswer("save `myVar` to `foo`", listOf("save", "`myVar`", "to", "foo")), // test the value wrapped in backticks
      TestAnswer("save true to `foo`", listOf("save", "true", "to", "foo")), // test true value
      TestAnswer("save false to `foo`", listOf("save", "false", "to", "foo")), // test false value

      TestAnswer("save the result to `buzz`", listOf("save", "result", "to", "buzz")), // test the `the` article, before the value
      TestAnswer("save a `pear` to `buzz`", listOf("save", "`pear`", "to", "buzz")), // test the `a` article, before the value
      TestAnswer("save an `apple` to `buzz`", listOf("save", "`apple`", "to", "buzz")), // test the `an` article, before the value
      TestAnswer("save 3 to the `buzz`", listOf("save", "3", "to", "buzz")), // test the `the` article, before the variable name
      TestAnswer("save 3 to a `buzz`", listOf("save", "3", "to", "buzz")), // test the `a` article, before the variable name
      TestAnswer("save 3 to an `apple`", listOf("save", "3", "to", "apple")), // test the `an` article, before the variable name

      TestAnswer("save the value 3 to `buzz`", listOf("save", "3", "to", "buzz")), // test the optional `value` word
      TestAnswer("save 3 to the variable `buzz`", listOf("save", "3", "to", "buzz")), // test the optional `variable` word

      TestAnswer("save `foo` incremented by 3 to `buzz`", listOf("save", "`foo`", "to", "buzz")), // test arbitrary text
      TestAnswer("save the result multiplied by `multiplier` to `foo`", listOf("save", "result", "to", "foo")), // test arbitrary text

      TestAnswer("sAvE tHe ResUlT tO `foo`", listOf("sAvE", "ResUlT", "tO", "foo")), // test case-insensitive
      TestAnswer("SAVE THE VALUe 3 To `buzz`", listOf("SAVE", "3", "To", "buzz")), // test case-insensitive
      TestAnswer("SaVE 3 tO `foo`", listOf("SaVE", "3", "tO", "foo")), // test case-insensitive
      TestAnswer("saVe tHE REsUlT DIVideD bY `divisor` TO `foo`", listOf("saVe", "REsUlT", "TO", "foo")), // test case-insensitive

      TestAnswer("save     `foo`   multiplied    by  3    to    `buzz`", listOf("save", "`foo`", "to", "buzz")), // test spacing
      TestAnswer("Save     3      tO    `foo`", listOf("Save", "3", "tO", "foo")), // test spacing
      TestAnswer("save     an  `apple`   to  `buzz`", listOf("save", "`apple`", "to", "buzz")), // test spacing
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
