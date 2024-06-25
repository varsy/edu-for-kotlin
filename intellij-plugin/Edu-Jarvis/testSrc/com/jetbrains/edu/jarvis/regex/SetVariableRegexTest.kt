package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase

class SetVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.SET_VARIABLE.regex

  override fun shouldMatchGroup() =
    listOf(
      // Test all SET synonyms
      TestAnswer("Set `foo` to 3", listOf("foo")),
      TestAnswer("assign `foo` to 3", listOf("foo")),
      TestAnswer("Give the variable `bar`", listOf("bar")),
      TestAnswer("initIalize `bar` to \"Hello world", listOf("bar")),
      // Test more complex sentences, different articles:
      TestAnswer("set the variable  called `myVar` to `foo` multiplied by 2", listOf("myVar")),
      TestAnswer("initialize `myVar` to `foo` multiplied by `bar`", listOf("myVar")),
      TestAnswer("initialize a variable `test` to \"this is a test\"", listOf("test")),
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

  fun `test valid set variable sentences`() = runTestShouldMatchGroup()
  fun `test invalid set variable sentences`() = runTestShouldNotMatch()

}
