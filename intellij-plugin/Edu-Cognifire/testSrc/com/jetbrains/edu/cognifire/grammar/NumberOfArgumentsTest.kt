package com.jetbrains.edu.cognifire.grammar

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.edu.cognifire.models.NamedFunction
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test

@RunWith(Parameterized::class)
class NumberOfArgumentsTest(private val sentence: String, private val expectedArgumentsList: List<String>) : BasePlatformTestCase() {
  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = listOf(
      arrayOf("", emptyList<String>()),
      arrayOf("1, 2, 3", listOf("1", "2", "3")),
      arrayOf("1, 2 and 3", listOf("1", "2", "3")),
      arrayOf("a,b,c,d", listOf("a", "b", "c", "d")),
      arrayOf("c and 2 and John Doe", listOf("c", "2", "John Doe")),
      arrayOf("""1, "two" and "three"""", listOf("1", "\"two\"", "\"three\"")),
      arrayOf(""""hello and welcome" and `myVar`""", listOf("\"hello and welcome\"", "`myVar`")),
      arrayOf("""mANDatory and Alice""", listOf("mANDatory", "Alice")),
      arrayOf("""A, B, and C""", listOf("A", "B", "C")),
      arrayOf(""""a and b and c", Bob""", listOf("\"a and b and c\"", "Bob")),
    )
  }

  @Test
  fun testGetNumberOfArguments() {
    TestCase.assertEquals(expectedArgumentsList, NamedFunction.getArguments(sentence))
  }
}
