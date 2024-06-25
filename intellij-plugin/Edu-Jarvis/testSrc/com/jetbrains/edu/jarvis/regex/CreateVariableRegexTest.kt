package com.jetbrains.edu.jarvis.regex

import com.jetbrains.edu.jarvis.TestGenerator.generateCreateVariable
import com.jetbrains.edu.jarvis.highlighting.AnnotatorRule
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.jarvis.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.learning.EduTestCase

class CreateVariableRegexTest : RegexTest, EduTestCase() {

  override val regex = AnnotatorRule.CREATE_VARIABLE.regex

  override fun shouldMatchGroup() =
    // Generated smoke tests
    List(NUMBER_OF_RUNS) {
      generateCreateVariable(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      // Test all CREATE synonyms:
      TestAnswer("Create `foo`", listOf("foo")),
      TestAnswer("declare `bar`", listOf("bar")),
      TestAnswer("set up `buz`", listOf("buz")),
      // Test more complex sentences, different articles:
      TestAnswer("CreaTe the Variable `test1`", listOf("test1")),
      TestAnswer("set up  an empty string `bar`", listOf("bar")),
      TestAnswer("Create a random   string called `testMe` ", listOf("testMe"))
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

  fun `test valid create variable sentences`() = runTestShouldMatchGroup()
  fun `test invalid create variable sentences`() = runTestShouldNotMatch()

}
