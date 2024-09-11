package com.jetbrains.edu.cognifire.regex

import com.jetbrains.edu.cognifire.TestGenerator.generateLoopExpression
import com.jetbrains.edu.cognifire.highlighting.undefinedidentifier.AnnotatorRule
import com.jetbrains.edu.cognifire.regex.RegexTest.Companion.MAX_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.cognifire.regex.RegexTest.Companion.MIN_IDENTIFIER_NAME_LENGTH
import com.jetbrains.edu.cognifire.regex.RegexTest.Companion.NUMBER_OF_RUNS
import com.jetbrains.edu.learning.EduTestCase
import org.junit.Test

class LoopExpressionRegexTest : RegexTest, EduTestCase() {
  override val regex = AnnotatorRule.LOOP_EXPRESSION.regex

  override fun shouldMatch() =
    // Generated smoke tests
    List(NUMBER_OF_RUNS) {
      generateLoopExpression(
        (MIN_IDENTIFIER_NAME_LENGTH..MAX_IDENTIFIER_NAME_LENGTH).random()
      )
    } + listOf(
      // minimal loop syntax
      TestAnswer("loop `item` in `collection`", listOf("item", "collection")),
      TestAnswer("for `index` in `list`", listOf("index", "list")),

      // optional elements may or may not be present
      TestAnswer("iterate through every item `index` in the array `arrayName`", listOf("index", "arrayName")),
      TestAnswer("loop over each element `item` in `collection`", listOf("item", "collection")),
      TestAnswer("go through all elements `item` in `list`", listOf("item", "list")),
      TestAnswer("for every element `item` in set `setName`", listOf("item", "setName")),
      TestAnswer("for every item `i` in the sequence `name`", listOf("i", "name")),

      // test spacing
      TestAnswer("for    every      item  `i`     in          sequence   `name`", listOf("i", "name")),
    )

  override fun shouldNotMatch() =
    listOf(
      "for `item`", // the part with `in' is missing
      "loop in `item`", // the identifier
      "loop in array", // no identifier
      "for item in `collection`", // no backticks
      "loop over `item` in collection", // no backticks
      "loop item in collection", // no backticks
    )

  @Test
  fun `test valid loop expressions`() = runTestShouldMatch()

  @Test
  fun `test invalid loop expressions`() = runTestShouldNotMatch()

}
