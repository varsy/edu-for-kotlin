package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.learning.EduTestCase

class StoreVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.STORE_VARIABLE.regex

  override fun shouldMatchGroup() =
    listOf(
      // Test all STORE synonyms
      TestAnswer("store 4 in `foo`", listOf("foo")),
      TestAnswer("stores `x` in `bar`", listOf("bar")),
      TestAnswer("stored \"it\" in `buz`", listOf("buz")),
      // Test more complex sentences, different articles:
      TestAnswer("store value 3 in `foo`", listOf("foo")),
      TestAnswer("store the value `foo` divided by 2 in the variable `bar`", listOf("bar")),
      TestAnswer("store `bar` in `foo2`", listOf("foo2")),
      TestAnswer("store \"hello\" in `foo3`", listOf("foo3")),
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

  fun `test valid store variable sentences`() = runTestShouldMatchGroup()
  fun `test invalid store variable sentences`() = runTestShouldNotMatch()

}
