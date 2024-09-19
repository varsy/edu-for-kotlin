package com.jetbrains.edu.kotlin.eduAssistant

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.eduAssistant.courses.createKotlinCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.eduAssistant.context.AuthorSolutionContext
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class StringExtractorTest : JdkCheckerTestBase() {

  @Test
  fun testStringsFromSolution() {
    val task = myCourse.findTask("lesson1", "task2")
    val context = AuthorSolutionContext.create(task) ?: error("Cannot build the author solution context")
    val strings = context.functionsToStringMap.values.flatten()
    assertEquals(stringsFromMainAndHiddenFile, strings)
  }

  @Test
  fun testStringsFromUserSolution() {
    val task = myCourse.findTask("lesson1", "task2")
    val taskProcessor = TaskProcessorImpl(task)
    val strings = taskProcessor.getStringsFromTask()
    assertEquals(stringsFromMainAndHiddenFile, strings)
  }

  override fun createCourse(): Course = createKotlinCourse()

  companion object {
    /*
    val stringTemplate = "string"
    fun greet(name: String) = "Hello, \$\{name\}!"
    fun main() {
        val a = "AA"
        val b = stringTemplate
        println(a)
        println("Hello!")
    }
   */
    private val stringsFromMainAndHiddenFile = listOf(
      """"Hello, \$\{name\}!"""",
      """"AA"""",
      """"Hello!"""",
      """"string"""",
      """"Printing...""""
    )
  }
}
