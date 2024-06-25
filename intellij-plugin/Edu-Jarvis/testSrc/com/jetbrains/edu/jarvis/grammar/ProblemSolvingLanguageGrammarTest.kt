package com.jetbrains.edu.jarvis.grammar

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.Test

@RunWith(Parameterized::class)
class ProblemSolvingLanguageGrammarTest(private val sentence: String) : BasePlatformTestCase() {

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = listOf(
      arrayOf("create `variable` equal to `value`"),
      arrayOf("create constant `variable` equal to `value`"),
      arrayOf("create constant variable called `width` equal 100"),
      arrayOf("""
        returns the following text: "Hello"
      """.trimIndent()),
      arrayOf("check if the length of the `userInput` does not equal 1"),
      arrayOf("""
        if the condition is true, prints "The length of your guess should be 1! Try again!" and return false
      """.trimIndent()),
      arrayOf("if 5 not in `list` then add 5 to `list`"),
      arrayOf("read the user input"),
      arrayOf("set value 10"),
      arrayOf("declare var named `counter` and set value 0"),
      arrayOf("loop each item in array do print item"),
      arrayOf("call function `multiply` with 2 3"),
      arrayOf("repeat until `isFinished` return true"),
      arrayOf("in a loop over all indices `i` in `secret` do"),
      arrayOf("add to `newUserWord` `secret[i]`"),
      arrayOf("get the user input and save the result to a variable named `humanYears`"),
      arrayOf("call the function `verifyHumanYearsInput` with `humanYears`"),
    )
  }

  @Test
  fun testParse() {
    assertNotNull(parseSentence(sentence))
  }
}
