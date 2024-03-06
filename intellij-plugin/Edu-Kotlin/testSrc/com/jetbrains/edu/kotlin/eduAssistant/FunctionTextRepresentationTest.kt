package com.jetbrains.edu.kotlin.eduAssistant

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.eduAssistant.courses.createKotlinCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.buildAuthorSolutionContext
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class FunctionTextRepresentationTest : JdkCheckerTestBase() {
  @Test
  fun testFunctionsSetTextRepresentation() {
    val task = getTaskAndCheckFunctions("lesson1", "task1", mainSignaturesList)
    // Check cache
    assertEquals(mainSignaturesList, task.getUniqueFunctionSignatures(MAIN_FILE_PATH))
  }

  @Test
  fun testFunctionsSetTextRepresentationOnLargeTask() {
    val preDefinedSymbolsSignaturesList = listOf(
      "getPictureWidth(picture: String): Int",
      "add(a: Int, b: Int): Int",
      "sum(numbers: IntArray): Int",
      "applyOperation(a: Int, b: Int, operation: Function2<Int, Int, Int>): Int",
      "getPrinter(): Function0<Unit>",
      "nullableLength(s: String?): Int?"
    )
    val task = getTaskAndCheckFunctions("lesson1", "task2", mainSignaturesList + preDefinedSymbolsSignaturesList)
    assertEquals(mainSignaturesList, task.getUniqueFunctionSignatures(MAIN_FILE_PATH))
    assertEquals(preDefinedSymbolsSignaturesList, task.getUniqueFunctionSignatures(UTIL_FILE_PATH))
    assertEmpty(task.getUniqueFunctionSignatures(TESTS_FILE_PATH))
  }

  @Test
  fun testFunctionsSetTextRepresentationFromTaskAndSolution() {
    val task = getTaskAndCheckFunctions("lesson2", "task2", listOf(MAIN_FUNCTION))
    val expectedFromSolution = listOf(MY_PRINT_FUNCTION, MAIN_FUNCTION)
    task.authorSolutionContext = task.buildAuthorSolutionContext()
    val authorSolutionContext = task.authorSolutionContext ?: error("Cannot build the author context")
    val actualFromSolution = authorSolutionContext.functionSignatures.map{ it.toString() }
    assertEquals(expectedFromSolution, actualFromSolution)
  }

  override fun createCourse(): Course = createKotlinCourse()

  private fun getTaskAndCheckFunctions(lessonName: String, taskName: String, expectedFunctions: List<String>): Task {
    val task = myCourse.findTask(lessonName, taskName)
    val taskProcessor = TaskProcessorImpl(task)
    val functionSet = taskProcessor.getFunctionsFromTask()
    assertEquals(expectedFunctions, functionSet)
    return task
  }

  private fun Task.getUniqueFunctionSignatures(taskFilePath: String): List<String> {
    val taskFile = taskFiles[taskFilePath] ?: error("Can't find task file $taskFilePath")
    val functionSignatures = taskFile.functionSignatures ?: return emptyList()
    return functionSignatures.map { it.toString() }
  }

  companion object {
    private const val GREET_FUNCTION = "greet(name: String): String"
    private const val MAIN_FUNCTION = "main(): Unit"
    private const val MY_PRINT_FUNCTION = "myPrint(): Unit"

    private const val MAIN_FILE_PATH = "src/main/kotlin/Main.kt"
    private val mainSignaturesList = listOf(GREET_FUNCTION, MAIN_FUNCTION)

    private const val UTIL_FILE_PATH = "src/main/kotlin/Util.kt"
    private const val TESTS_FILE_PATH = "test/Tests.kt"
  }
}
