package com.jetbrains.edu.aiHints.kotlin

import com.jetbrains.edu.aiHints.core.TaskProcessorImpl
import com.jetbrains.edu.aiHints.core.context.AuthorSolutionContext
import com.jetbrains.edu.aiHints.core.context.authorSolutionContext
import com.jetbrains.edu.aiHints.kotlin.courses.createKotlinCourse
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.junit.Test

class TaskProcessorTest : JdkCheckerTestBase() {

  @Test
  fun testFailedTestsAndMessage() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson3", "task1")
    myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    checkTask(task)
    val testFailureContext = TaskProcessorImpl(task).getTestFailureContext()
    testFailureContext?.name?.let {
      // We have different behaviour on different IDEA versions
      assertTrue("Actual failed test name is $it", it in listOf("Test class Tests:testSolution", "Tests:testSolution"))
    }
    testFailureContext?.message?.let {
      // We have different behaviour on different IDEA versions
      assertTrue("Actual failed test message is $it", it in listOf("foo() should return 42", "Execution failed"))
    }
  }

  @Test
  fun testTaskTextRepresentation() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson4", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    val taskText = taskProcessor.getTaskTextRepresentation()
    val expected = """
It's time to write your first program in Kotlin! Task Change the output text into Hello! and run the program. fun main() {
    // Some code here
}
 Image Feedback Survey The survey is anonymous and should take no more than 5 minutes to complete.
    """.trimIndent()
    assertEquals(expected, taskText)
  }

  @Test
  fun testHintsTextRepresentation() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson4", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    val hints = taskProcessor.getHintsTextRepresentation()
    val expected = listOf(
      """
        To run your program, you need to open the Main.kt file and click on the green triangle near the main function. Then, the output of the program will be shown in the console:
      """.trimIndent(), """
        If for some reason the survey link is unclickable, you can copy the full link here: https://surveys.jetbrains.com/link
      """.trimIndent()
    )
    assertEquals(expected, hints)
  }

  @Test
  fun testRecommendsImplementingShortFunction() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val state = project.eduState ?: error("State was not found")
    NavigationUtils.navigateToTask(project, task, state.task)
    val taskProcessor = TaskProcessorImpl(task)
    task.authorSolutionContext = AuthorSolutionContext.create(task)
    val generatedCode = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
    """.trimIndent()
    assertEquals(
      generatedCode,
      taskProcessor.getShortFunctionFromSolutionIfRecommended(generatedCode, "greet").toString()
    )
  }

  @Test
  fun testExtractRequiredFunctionsFromCodeHint() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson5", "task2")
    val state = project.eduState ?: error("State was not found")
    NavigationUtils.navigateToTask(project, task, state.task)
    val taskProcessor = TaskProcessorImpl(task)
    val codeHint = """
      package jetbrains.kotlin.course.first.date

      fun generateSecret() = "ABCDEFG"
      
      fun main() {
          println("Hello!")
      }
    """.trimIndent()
    val updatedCodeHint = """
      fun main() {
          println("Hello!")
      }
    """.trimIndent()
    assertEquals(updatedCodeHint, taskProcessor.extractRequiredFunctionsFromCodeHint(codeHint))
  }

  override fun createCourse(): Course = createKotlinCourse()
}
