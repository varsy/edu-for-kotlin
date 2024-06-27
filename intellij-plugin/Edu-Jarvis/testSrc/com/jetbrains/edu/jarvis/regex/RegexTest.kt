package com.jetbrains.edu.jarvis.regex

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Base class for testing regular expressions.
 */
interface RegexTest {

  /**
   * The [Regex] that is being tested.
   */
  val regex: Regex

  /**
   * Returns a [Collection] of test cases that should **not** match with the `regex`.
   */
  fun shouldNotMatch(): Collection<String> = emptyList()

  /**
   * Returns a [Collection] of test cases that should match with the `regex` and have the required capturing groups.
   */
  fun shouldMatch(): Collection<TestAnswer> = emptyList()

  fun runTestShouldNotMatch() =
    shouldNotMatch().forEach { assertFalse(regex.matches(it)) }

  fun runTestShouldMatch() =
    shouldMatch().forEach { assertTrue(regex.find(it.input)!!.groups.values() == it.answer) }

  fun MatchGroupCollection.values() = this.mapNotNull { it?.value }.drop(1)

  companion object {
    const val NUMBER_OF_RUNS = 10

    const val MIN_IDENTIFIER_NAME_LENGTH = 4
    const val MAX_IDENTIFIER_NAME_LENGTH = 30

  }


}
