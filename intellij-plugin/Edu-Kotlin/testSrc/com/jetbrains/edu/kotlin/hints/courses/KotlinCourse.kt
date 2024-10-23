package com.jetbrains.edu.kotlin.hints.courses

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.jetbrains.kotlin.idea.KotlinLanguage

fun createKotlinCourse() = course(language = KotlinLanguage.INSTANCE) {
  lesson {
    eduTask {
      kotlinTaskFile(
        "src/main/kotlin/Main.kt", """
          fun greet(name: String) = "Hello, \${'$'}\{name\}!"
          fun main() {
              println("Hello!")
          }
        """
      )
    }
    eduTask {
      kotlinTaskFile(
        "src/main/kotlin/Main.kt", """
          val stringTemplate = "string"
          fun greet(name: String) = "Hello, \$\{name\}!"
          fun main() {
              val a = "AA"
              val b = stringTemplate
              println(a)
              println("Hello!")
          }
        """
      )
      kotlinTaskFile(
        "src/main/kotlin/Util.kt", """
          val borderSymbol = '#'
          val separator = ' '
          val newLineSymbol = System.lineSeparator()
          fun getPictureWidth(picture: String) = picture.lines().maxOfOrNull { it.length } ?: 0
          fun add(a: Int, b: Int): Int {
              return a + b
          }
          fun sum(vararg numbers: Int): Int {
              return numbers.sum()
          }
          fun applyOperation(a: Int, b: Int, operation: (Int, Int) -> Int) = operation(a, b)
          fun getPrinter(): () -> Unit = { println("Printing...") }
          fun nullableLength(s: String?) = s?.length
        """
      )
      kotlinTaskFile(
        "test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test
          class Test {
              @Test
              fun testSolution() {
              }
          }
        """
      )
    }
  }
  frameworkLesson {
    theoryTask {
      kotlinTaskFile(
        "src/main/kotlin/Main.kt", """
          fun main() {
            println("Hello!")
          }
        """
      )
    }
    eduTask {
      kotlinTaskFile(
        "src/main/kotlin/Main.kt", """
          fun myPrint() {
            println("Hello!")
          }
          fun main() {
            myPrint()
          }
        """
      )
    }
  }
  lesson {
    eduTask {
      kotlinTaskFile("src/Main.kt", "fun foo(): Int = 5")
      kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test
          class Tests {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
    }
  }
  lesson {
    eduTask(taskDescription = """
      It's time to write your first program in Kotlin!
      
      ### Task
      
      Change the output text into `Hello!` and run the program.
      ```kotlin
      fun main() {
          // Some code here
      }
      ```
      
      <div class="hint" title="Click me to learn how to run your program">
      
      To run your program, you need to open the `Main.kt` file and click on the **green triangle** near the `main` function.
      Then, the output of the program will be shown in the console:
      ![Program entry point and console](../../utils/src/main/resources/images/run_example.gif "Program entry point and console")
      </div>
          
      #### Image
      
      ![Image](../../utils/src/main/resources/images/image.gif "Image")  
      
      # Feedback Survey
      
      The [survey](https://surveys.jetbrains.com/link) is anonymous and should take no more than 5 minutes to complete.    
      
      <div class="hint">
      If for some reason the survey link is unclickable, you can copy the full link here:
      https://surveys.jetbrains.com/link
      </div>
    """.trimIndent(), taskDescriptionFormat = DescriptionFormat.MD) {
      kotlinTaskFile(
        "src/main/kotlin/Main.kt", """
          fun main() {
              println("Hello!")
          }
        """
      )
    }
  }
  frameworkLesson {
    theoryTask {
      kotlinTaskFile(
        "src/main/kotlin/Main.kt", """
          package jetbrains.kotlin.course.first.date

          fun generateSecret() = "ABCD"
          
          fun main() {
              println("My first program!")
          }
        """
      )
    }
    eduTask {
      kotlinTaskFile("src/main/kotlin/Main.kt", """
          package jetbrains.kotlin.course.first.date

          fun generateSecret() = "ABCD"
          
          fun main() {
              println("Hello!")
          }
        """)
    }
  }
}
