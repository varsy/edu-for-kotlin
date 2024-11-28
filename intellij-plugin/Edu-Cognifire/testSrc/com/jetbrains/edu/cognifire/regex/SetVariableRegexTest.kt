package com.jetbrains.edu.cognifire.regex

import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class SetVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.SET_VARIABLE.regex

  override fun shouldMatch() =
    listOf(
      TestAnswer("set `foo` to 3", listOf("set", "foo", "to", "3")), // test the `set` verb
      TestAnswer("assign `foo` to 3", listOf("assign", "foo", "to", "3")), // test the `assign` verb
      TestAnswer("give `bar` to 3", listOf("give", "bar", "to", "3")), // test the `give` verb

      TestAnswer("set the `buzz` to 3", listOf("set", "buzz", "to", "3")), // test the `the` article
      TestAnswer("set a `buzz` to 3", listOf("set", "buzz", "to", "3")), // test the `a` article
      TestAnswer("set an `apple` to 3", listOf("set", "apple", "to", "3")), // test the `an` article

      TestAnswer("set `foo` to result", listOf("set", "foo", "to", "result")), // test the `result` word
      TestAnswer("set `foo` to 3321", listOf("set",  "foo", "to", "3321")), // test the number
      TestAnswer("set `greeting` to \"hello\"", listOf("set", "greeting", "to", "\"hello\"")), // test the string value
      TestAnswer("set `foo` to `myVar`", listOf("set", "foo", "to", "`myVar`")), // test the value wrapped in backticks
      TestAnswer("set `foo` to true", listOf("set", "foo", "to", "true")), // test true value
      TestAnswer("set `foo` to false", listOf("set", "foo", "to", "false")), // test false value

      TestAnswer("set the variable `buzz` to 3", listOf("set", "buzz", "to", "3")), // test the optional `variable` word
      TestAnswer("set the variable called `buzz` to 3", listOf("set", "buzz", "to", "3")), // test the optional `called` word

      TestAnswer("SET `foo` to 3", listOf("SET", "foo", "to", "3")), // test case-insensitive
      TestAnswer("Set The `buzz` To 3", listOf("Set", "buzz", "To", "3")), // test case-insensitive
      TestAnswer("GiVE tHE varIaBLe `buzz` tO 3", listOf("GiVE", "buzz", "tO", "3")), // test case-insensitive
      TestAnswer("SEt THE vaRiABLE cALlEd `buzz` TO 3", listOf("SEt", "buzz", "TO", "3")), // test case-insensitive

      TestAnswer("give   `bar`   to   3", listOf("give", "bar", "to", "3")), // test spacing
      TestAnswer("set  a   `buzz`   to      3    ", listOf("set", "buzz", "to", "3")), // test spacing
      TestAnswer("set   the   variable    called `buzz`    to   3  ", listOf("set", "buzz", "to", "3")), // test spacing
      )

  override fun shouldNotMatch() =
    listOf(
      "create `foo()`", // wrong SET synonym
      "give something to the `test`", // incorrect grammar
      "set the v t`test", // incorrect grammar
      "store the `2test", // missing closing backtick
      "create `test()`", // cannot create a function
      "seet `foo`", // typo in the word "seet"
      "se bar", // invalid grammar
    )

  @Test
  fun `test valid set variable sentences`() = runTestShouldMatch()

  @Test
  fun `test invalid set variable sentences`() = runTestShouldNotMatch()

}
