package com.jetbrains.edu.aiDebugging.kotlin

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.aiDebugging.core.breakpoint.IntermediateBreakpointProcessor
import org.junit.Test
import org.junit.Before
import com.jetbrains.edu.learning.EduTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import com.jetbrains.edu.learning.StudyTaskManager
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class IntermediateBreakpointTest(
  private val wrongCodeLines: List<Int>,
  private val intermediateBreakpoints: List<Int>,
) : EduTestCase() {

  private var virtualFile: VirtualFile? = null

  @Before
  fun initialise() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    virtualFile = task.taskFiles.values.first().getVirtualFile(project) ?: error("Can't find virtual file for `${task.name}` task")
  }

  @Test
  fun `test breakpoints by heuristics`() {
    virtualFile?.let { file ->
      assertEquals(
        intermediateBreakpoints.toSet(),
        runReadAction {
          IntermediateBreakpointProcessor.calculateIntermediateBreakpointPositions(file, wrongCodeLines, project, language).toSet()
        }
      )
    }
  }

  override fun createCourse() {
    StudyTaskManager.getInstance(project).course = courseWithFiles(courseMode = CourseMode.STUDENT) {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile(
            name = "Main.kt",
            text = """
              object Task {
                  fun foo(name: String) {
                      var a = 10
                      if (a > 5 && a < 5) {
                          println("Valid")
                          a = a + name.toInt()
                      } else if (a > 10) {
                          println("Valid")
                      } else println("Invalid")
        
                      val numbers = intArrayOf(1, 2, 3, 4, 5)
                      for (i in 0..numbers.size) {
                          println("Number: " + numbers[i])
                      }
        
                      when (name) {
                          "John" -> println("Hello, John!")
                          "Mark" -> println("Hello, Mark!")
                          else -> { 
                            println("Hello, stranger!")
                          }
                      }
        
                      while (a > 0) {
                          a = a - 1
                      }
                  }
                  
                  fun main() {
                      foo("John")
                  }
              }
            """.trimIndent()
          )
        }
      }
    }
  }

  companion object {

    @Parameterized.Parameters(name = "{1}")
    @JvmStatic
    fun data(): Collection<Array<Any>> = listOf(
      arrayOf(listOf(1), functionStartLines + functionCallLines), // test function
      arrayOf(listOf(2), propertyUsesLines + functionCallLines), // test property
      arrayOf(listOf(5), parameterUsesLines + propertyUsesLines + functionCallLines), // test BinaryExpression
      arrayOf(listOf(11), listOf(12) + functionCallLines), // test for
      arrayOf(listOf(3), listOf(4, 7, 8) + functionCallLines), // test if
      arrayOf(listOf(15), listOf(16, 17, 19) + functionCallLines), // test when
      arrayOf(listOf(23), propertyUsesLines + functionCallLines), // test while
      arrayOf(listOf(1, 2, 5), functionStartLines + functionCallLines + parameterUsesLines + propertyUsesLines), // test multiple lines
    )

    private val functionCallLines = listOf(29)

    private val propertyUsesLines = listOf(3, 5, 6, 23, 24)

    private val parameterUsesLines = listOf(6, 15)

    private val functionStartLines = listOf(2)

    private val language = KotlinLanguage.INSTANCE
  }
}
