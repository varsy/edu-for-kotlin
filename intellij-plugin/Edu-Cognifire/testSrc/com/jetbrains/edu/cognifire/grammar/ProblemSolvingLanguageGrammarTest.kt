package com.jetbrains.edu.cognifire.grammar

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ProblemSolvingLanguageGrammarTest(
  private val testInput: String
) : BasePlatformTestCase() {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "parse: {0}")
    fun testData() = listOf(
      "create `variable` equal to `value`",
      "create constant `variable` equal to `value`",
      "create constant variable called `width` equal 100",
      """returns the following text: "Hello"""",
      "check if the length of the `userInput` does not equal 1",
      """if the condition is true, prints "The length of your guess should be 1! Try again!" and return false""",
      "if 5 not in `list` then add 5 to `list`",
      "read the user input",
      "set value 10",
      "declare var named `counter` and set value 0",
      "loop each item in array do print item",
      "call function `multiply` with 2 3",
      "repeat until `isFinished` return true",
      "in a loop over all indices `i` in `secret` do",
      "add to `newUserWord` `secret[i]`",
      "get the user input and save the result to a variable named `humanYears`",
      "call the function `verifyHumanYearsInput` with `humanYears`",
      "read the user input. save it to `userInput`"
    ).map { arrayOf(it) }
  }

  @Test
  fun `test sentence parsing`() {
    assertNotNull(testInput.parse())
  }
}