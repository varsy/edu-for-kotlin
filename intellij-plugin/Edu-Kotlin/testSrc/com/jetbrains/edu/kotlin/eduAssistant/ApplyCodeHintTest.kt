package com.jetbrains.edu.kotlin.eduAssistant

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.eduAssistant.courses.createKotlinCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class ApplyCodeHintTest : JdkCheckerTestBase() {
  @Test
  fun testApplyCodeHint() {
    val codeHint = """
      fun main() {
          println("Hello!")
          val firstUserAnswer = readlnOrNull()
      }
    """.trimIndent()
    val updatedUserCode = """
      $greetFunction
      $codeHint
    """.trimIndent()
    applyHint(updatedUserCode, codeHint)
  }

  @Test
  fun testApplyCodeHintNewFunction() {
    val updatedUserCode = """
      $greetFunction
      $simpleMain
      
      $newFunction
    """.trimIndent()
    applyHint(updatedUserCode, newFunction)
  }

  @Test
  fun testApplyCodeHintAlreadyImplementedFunction() {
    val updatedUserCode = """
      $greetFunction
      $simpleMain
    """.trimIndent()
    applyHint(updatedUserCode, greetFunction)
  }

  override fun createCourse(): Course = createKotlinCourse()

  private fun applyHint(expectedUserCode: String, codeHint: String) {
    val task = myCourse.findTask("lesson1", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    assertEquals(expectedUserCode.reformatCode(project), taskProcessor.applyCodeHint(codeHint))
  }

  companion object {
    private val simpleMain = """
      fun main() {
          println("Hello!")
      }
    """.trimIndent()

    private val greetFunction = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
    """.trimIndent()

    private val newFunction = """
      fun newFunction() {
          val firstUserAnswer = readlnOrNull()
      }
    """.trimIndent()
  }
}
