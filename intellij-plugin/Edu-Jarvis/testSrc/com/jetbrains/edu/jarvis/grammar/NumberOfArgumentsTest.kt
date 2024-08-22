package com.jetbrains.edu.jarvis.grammar

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.edu.jarvis.models.NamedFunction
import junit.framework.TestCase
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test

@RunWith(Parameterized::class)
class NumberOfArgumentsTest(val sentence: String, val expectedNumberOfArguments: Int) : BasePlatformTestCase() {
  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = listOf(
      arrayOf("", 0),
      arrayOf("1, 2, 3", 3),
      arrayOf("1, 2 and 3", 3),
      arrayOf("a,b,c,d", 4),
      arrayOf("c and 2 and John Doe", 3),
      arrayOf("""1, "two" and "three"""", 3),
      arrayOf(""""hello and welcome" and `myVar`""", 2),
      arrayOf("""mANDatory and Alice""", 2),
      arrayOf("""A, B, and C""", 3),
      arrayOf(""""a and b and c", Bob""", 2)
    )
  }

  @Test
  fun testGetNumberOfArguments() {
    TestCase.assertEquals(expectedNumberOfArguments, NamedFunction.getNumberOfArguments(sentence))
  }
}
