package com.jetbrains.edu.kotlin.eduAssistant

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.eduAssistant.courses.createKotlinCourse
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.authorSolutionContext
import com.jetbrains.edu.learning.courseFormat.ext.updateContent
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.createAuthorSolutionContext
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.testAction
import com.jetbrains.educational.ml.hints.processors.TaskProcessor
import org.junit.Test

class FilesDiffProviderTest : JdkCheckerTestBase() {
  /*
    Test the initial state of the task in a lesson by comparing the state from the previous task and the current one.

    Initial state:
    fun greet(name: String) = "Hello, \$\{name\}!"
    fun main() {
        println("Hello!")
    }

    Current state:
    val stringTemplate = "string"
    fun greet(name: String) = "Hello, \$\{name\}!"
    fun main() {
        val a = "AA"
        val b = stringTemplate
        println(a)
        println("Hello!")
    }

    Expected changes: main function
    fun main() {
      val a = "AA"
      val b = stringTemplate
      println(a)
      println("Hello!")
    }
  * */
  @Test
  fun testInitialState() {
    val task = initCourseAndGetTaskForTests()
    val taskProcessor = TaskProcessorImpl(task)
    testAction(NextTaskAction.ACTION_ID)
    assertEquals(initialFunction.reformatCode(project), taskProcessor.getSubmissionTextRepresentationAndReformat(project))
  }

  /*
    Test adding new function to the student code.

    Initial state (before changes):
    fun greet(name: String) = "Hello, \$\{name\}!"
    fun main() {
        println("Hello!")
    }

    Current state (after changes):
    fun greet(name: String) = "Hello, \$\{name\}!"
    fun main() {
        val a = "AA"
        val b = stringTemplate
        println(a)
        println("Hello!")
    }
    fun newFunction() {
        println("Hello world!")
    }

    Expected changes: main and newFunction function
    fun main() {
        val a = "AA"
        val b = stringTemplate
        println(a)
        println("Hello!")
    }

    fun newFunction() {
        println("Hello world!")
    }
   */
  @Test
  fun testAddingNewFunction() {
    val task = initCourseAndGetTaskForTests()
    val taskProcessor = TaskProcessorImpl(task)
    val taskFile = task.taskFiles["src/main/kotlin/Main.kt"] ?: error("Can't find Main.kt")
    val newContent = """
        $greetFunction
        $initialFunction
        $newFunction
      """.trimIndent()
    taskFile.updateContent(project, newContent)
    val changedFunctions = listOf(initialFunction, newFunction).joinToString(separator = System.lineSeparator()).reformatCode(project)
    assertEquals(changedFunctions, taskProcessor.getSubmissionTextRepresentationAndReformat(project))
  }

  override fun createCourse(): Course = createKotlinCourse()

  private fun TaskProcessor.getSubmissionTextRepresentationAndReformat(project: Project) =
    getSubmissionTextRepresentation()?.reformatCode(project)

  private fun initCourseAndGetTaskForTests(): Task {
    val task = myCourse.findTask("lesson1", "task2")
    task.authorSolutionContext = createAuthorSolutionContext(task)
    return task
  }

  companion object {
    private val initialFunction = """
        fun main() {
            val a = "AA"
            val b = stringTemplate
            println(a)
            println("Hello!")
        }
      """.trimIndent()

    private val newFunction = """
        fun newFunction() {
            println("Hello world!")
        }
      """.trimIndent()

    private val greetFunction = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
    """.trimIndent()
  }
}
