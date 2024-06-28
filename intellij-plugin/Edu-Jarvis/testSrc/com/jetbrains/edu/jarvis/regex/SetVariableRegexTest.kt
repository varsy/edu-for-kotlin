package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class SetVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.SET_VARIABLE.regex

  override fun shouldMatch() =
    listOf(
      TestAnswer("set `foo`", listOf("foo")), // test the `set` verb
      TestAnswer("assign `foo`", listOf("foo")), // test the `assign` verb
      TestAnswer("give `bar`", listOf("bar")), // test the `give` verb
      TestAnswer("initialize `bar`", listOf("bar")), // test the `initialize` verb

      TestAnswer("set the `buzz`", listOf("buzz")), // test the `the` article
      TestAnswer("set a `buzz`", listOf("buzz")), // test the `a` article
      TestAnswer("set an `apple`", listOf("apple")), // test the `an` article

      TestAnswer("initialize the variable `buzz`", listOf("buzz")), // test the optional `variable` word
      TestAnswer("set the variable called `buzz`", listOf("buzz")), // test the optional `called` word

      TestAnswer("SET `foo`", listOf("foo")), // test case-insensitive
      TestAnswer("Set The `buzz`", listOf("buzz")), // test case-insensitive
      TestAnswer("iNiTIAliZe tHE varIaBLe `buzz`", listOf("buzz")), // test case-insensitive
      TestAnswer("SEt THE vaRiABLE cALlEd `buzz`", listOf("buzz")), // test case-insensitive

      TestAnswer("give   `bar`", listOf("bar")), // test spacing
      TestAnswer("set  a   `buzz`", listOf("buzz")), // test spacing
      TestAnswer("set   the   variable    called `buzz`", listOf("buzz")), // test spacing
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
