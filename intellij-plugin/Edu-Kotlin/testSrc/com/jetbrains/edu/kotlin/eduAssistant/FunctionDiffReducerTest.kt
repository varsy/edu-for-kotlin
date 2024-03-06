package com.jetbrains.edu.kotlin.eduAssistant

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerFixture
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.eduAssistant.courses.createKotlinCourse
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.findTask
import org.junit.Test

/**
 * Tests reductions of code modifications that the llm-model has generated.
 * Each test represents a scenario that the function should be able to reduce.
 */
class FunctionDiffReducerTest : JdkCheckerTestBase() {

  /**
   * Transformation type: Additive Statement Isolation
   * Applies to the Function
   * Changes: only the Function definition remains, the body has been replaced by TODO statement, since it's a new Function
   */
  @Test
  fun testNewFunctionInCodeHint() {
    val codeStr = """
      $greetFunction
      $mainFunction
    """.trimIndent()
    val codeHint = """
      $greetFunction
      fun newFunction() {
        val a = "AA"
        println(a)
        println("Hello!")
      }
      $mainFunction
    """.trimIndent()
    val newEmptyFunction = """
      fun newFunction() {
          TODO("Not yet implemented")
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHint, newEmptyFunction, NEW_FUNCTION_NAME)
  }

  /**
   * Transformation type: Intrinsic Structure Modification Focus
   * Applies to the Function
   * Changes: only the addition of a new argument remains, the body of the Function remains unchanged
   */
  @Test
  fun testFunctionByKeepingAddedArgumentInCodeHint() {
    val codeStr = """
      $greetFunction
      $mainFunction
    """.trimIndent()
    val codeHint = """
      fun greet(name: String, age: String) = "Hello, \${'$'}\{name\}\${'$'}\{age\}!"
      $mainFunction
    """.trimIndent()
    val greetFunctionWithNewArgumentAndUnchangedBody = """
      fun greet(name: String, age: String) = "Hello, \${'$'}\{name\}!"
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHint, greetFunctionWithNewArgumentAndUnchangedBody, GREET_FUNCTION_NAME)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the Function
   * Changes: only adding one line in the Function body remains
   */
  @Test
  fun testAdditionOfLinesInFunctionBodyInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
          println("Hello!")
          println("End")
      }
    """.trimIndent()
    val mainFunctionWithNewLine = """
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
          println("End")
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintVar, mainFunctionWithNewLine)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the Function
   * Changes: only changing one line in the Function body remains - var to val
   */
  @Test
  fun testVarToValModificationInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
          println("Hello!")
          var firstUserAnswer: String = ""
          var secondUserAnswer: String = ""
          var thirdUserAnswer: String = ""
      }
    """.trimIndent()
    val mainFunctionWithOneValAndTwoVar = """
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
          var secondUserAnswer: String = ""
          var thirdUserAnswer: String = ""
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintVar, mainFunctionWithOneValAndTwoVar)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the Function
   * Changes: only changing one line in the Function body remains - fixed typo
   */
  @Test
  fun testTypoFixInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
          println("Hello!")
          val fisrtUserAnswer: String = ""
      }
    """.trimIndent()
    val mainFunctionWithFixedTypo = """
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintVar, mainFunctionWithFixedTypo)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the Function
   * Changes: only adding one line in the empty Function remains
   */
  @Test
  fun testAdditionOfLinesForEmptyFunctionBodyInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintVar, mainFunction)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the Function
   * Changes: only adding one line in the empty Function with TODO body remains
   */
  @Test
  fun testAdditionOfLinesForTODOFunctionBodyInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        TODO("Not implemented")
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintVar, mainFunction)
  }

  /**
   * Transformation type: Additive Statement Isolation
   * Applies to the While statement
   * Changes: only the While statement without body remains, since it's a new While statement
   */
  @Test
  fun testWhileStatementAdditionInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        var complete: Boolean
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintDoWhile, mainFunctionWithEmptyWhileStatement)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the While statement
   * Changes: only adding one line in the While statement body remains
   */
  @Test
  fun testAdditionOfFirstLineToWhileStatementInCodeHint() {
    val codeStr = """
      $greetFunction
      $mainFunctionWithEmptyWhileStatement
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintDoWhile, mainFunctionWithWhileStatementWithOneLineBody)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the While statement
   * Changes: only adding one line in the While statement body remains
   */
  @Test
  fun testAdditionOfOneNewLineToWhileStatementInCodeHint() {
    val codeStr = """
      $greetFunction
      $mainFunctionWithWhileStatementWithOneLineBody
    """.trimIndent()
    val mainFunctionWithWhileStatementWithTwoLinesBody = """
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
        } while (!complete)
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintDoWhile, mainFunctionWithWhileStatementWithTwoLinesBody)
  }

  /**
   * Transformation type: Intrinsic Structure Modification Focus
   * Applies to the While statement
   * Changes: only the change of the While condition remains, the body of the While statement remains unchanged
   */
  @Test
  fun testChangingWhileConditionInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
        } while (complete)
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintDoWhile, mainFunctionWithWhileStatementWithOneLineBody)
  }

  /**
   * Transformation type: Additive Statement Isolation
   * Applies to the If statement
   * Changes: only the If statement without body remains, since it's a new If statement
   */
  @Test
  fun testIfConditionAdditionToWhileInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
        } while (!complete)
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintIf, mainFunctionWithEmptyIfStatement)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the If statement
   * Changes: only adding one line in the If statement body remains
   */
  @Test
  fun testIfBodyAdditionToWhileInCodeHint() {
    val codeStr = """
      $greetFunction
      $mainFunctionWithEmptyIfStatement
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintIf, mainFunctionWithIfStatementWithOneLineBody)
  }

  /**
   * Transformation type: Intrinsic Structure Modification Focus
   * Applies to the If statement
   * Changes: only the change of the If condition remains, the body of the If statement remains unchanged
   */
  @Test
  fun testIfConditionChangeInWhileInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (!isLost(complete, attempts, maxAttemptsCount)) {
            }
        } while (!complete)
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintIf, mainFunctionWithEmptyIfStatement)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the If statement
   * Changes: only adding one line in the If statement body remains
   */
  @Test
  fun testAdditionOfLineToIfBodyInCodeHint() {
    val codeStr = """
      $greetFunction
      $mainFunctionWithIfStatementWithOneLineBody
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintIf, mainFunctionWithIfStatementWithTwoLinesBody)
  }

  /**
   * Transformation type: Additive Statement Isolation
   * Applies to the Else statement
   * Changes: only the Else statement without body remains, since it's a new Else statement
   */
  @Test
  fun testElseIfStatementAdditionToWhileInCodeHint() {
    val codeStr = """
      $greetFunction
      $mainFunctionWithIfStatementWithTwoLinesBody
    """.trimIndent()
    val mainFunctionWithIfElseStatement = """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            }else if (isWon(complete, attempts, maxAttemptsCount)) {
                  }
        } while (!complete)
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintIf, mainFunctionWithIfElseStatement)
  }

  /**
   * Transformation type: Additive Statement Isolation
   * Applies to the Else statement
   * Changes: the Else statement with body remains, since it's short
   */
  @Test
  fun testElseStatementAdditionToWhileInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        val word = "Hello"
        if (word == "Hello") {
            println("Hello")
        } else if (word == "Hello!") {
            println("Hello!")
        }
      }
    """.trimIndent()
    val codeHint = """
      $greetFunction
      $mainFunctionWithDefaultIfElseStatement
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHint, mainFunctionWithDefaultIfElseStatement)
  }

  /**
   * Transformation type: Additive Statement Isolation
   * Applies to the For statement
   * Changes: only the For statement without body remains, since it's a new For statement
   */
  @Test
  fun testForConditionAdditionInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        var newUserWord = ""
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintFor, mainFunctionWithEmptyForStatement)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the For statement
   * Changes: adding one expression in the For statement body remains
   */
  @Test
  fun testForBodyAdditionInCodeHint() {
    val codeStr = """
      $greetFunction
      $mainFunctionWithEmptyForStatement
    """.trimIndent()
    val mainFunctionWithForStatement = """
      fun main() {
        var newUserWord = ""
        for (i in secret.indices) {
            newUserWord += if (secret[i] == guess) {
                "secret[i]"
            } else {
                "currentUserWord[i * 2]"
            }
        }
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintFor, mainFunctionWithForStatement)
  }

  /**
   * Transformation type: Additive Statement Isolation
   * Applies to the statements Return and When
   * Changes: only the When statement without body remains, since it's a When while statement
   * (return is ignored and the following is processed)
   */
  @Test
  fun testWhenConditionAdditionAfterReturnInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        val trimmedPicture = trimPicture(picture)
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintWhen, mainFunctionWithEmptyWhenStatement)
  }

  /**
   * Transformation type: Intrinsic Structure Modification Focus
   * Applies to the statements Return and When
   * Changes: only the change of the When condition remains, the body of the When statement remains unchanged
   */
  @Test
  fun testWhenConditionChangeInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter1) {
        }
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintWhen, mainFunctionWithEmptyWhenStatement)
  }

  /**
   * Transformation type: Internal Body Change Detection
   * Applies to the statements Return and When
   * Changes: adding one Else entry in the When statement remains
   */
  @Test
  fun testElseAdditionToWhenConditionInCodeHint() {
    val codeStr = """
      $greetFunction
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter) {
          "borders" -> applyBordersFilter(trimmedPicture)
          "squared" -> applySquaredFilter(trimmedPicture)
        }
      }
    """.trimIndent()
    val mainFunctionWithWhenStatementWithElseEntry = """
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter) {
          "borders" -> applyBordersFilter(trimmedPicture)
          "squared" -> applySquaredFilter(trimmedPicture)
          else -> error("Unexpected filter")
        }
      }
    """.trimIndent()
    reduceChangesInCodeHint(codeStr, codeHintWhen, mainFunctionWithWhenStatementWithElseEntry)
  }

  private fun reduceChangesInCodeHint(codeStr: String, codeHint: String, updatedCodeHint: String, functionName: String = MAIN_FUNCTION_NAME) {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    assertEquals(
      updatedCodeHint.reformatCode(project),
      taskProcessor.reduceChangesInCodeHint(codeStr, codeHint, functionName).reformatCode(project)
    )
  }

  override fun createCheckerFixture() = JdkCheckerFixture()

  override fun createCourse() = createKotlinCourse()

  companion object {
    private val greetFunction = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
    """.trimIndent()

    private val mainFunction = """
      fun main() {
          println("Hello!")
      }
    """.trimIndent()

    private val mainFunctionWithEmptyWhileStatement = """
      fun main() {
        var complete: Boolean
        do {
        } while (!complete)
      }
    """.trimIndent()

    private val mainFunctionWithWhileStatementWithOneLineBody = """
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
        } while (!complete)
      }
    """.trimIndent()

    private val mainFunctionWithEmptyIfStatement = """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
            }
        } while (!complete)
      }
    """.trimIndent()

    private val mainFunctionWithIfStatementWithOneLineBody = """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
            }
        } while (!complete)
      }
    """.trimIndent()

    private val mainFunctionWithIfStatementWithTwoLinesBody = """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            }
        } while (!complete)
      }
    """.trimIndent()

    private val mainFunctionWithDefaultIfElseStatement = """
      fun main() {
        val word = "Hello"
        if (word == "Hello") {
            println("Hello")
        } else if (word == "Hello!") {
            println("Hello!")
        } else {
          println("Hello World!")
        }
      }
    """.trimIndent()

    private val mainFunctionWithEmptyForStatement = """
      fun main() {
        var newUserWord = ""
         for (i in secret.indices) {
        }
      }
    """.trimIndent()

    private val mainFunctionWithEmptyWhenStatement = """
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter) {
        }
      }
    """.trimIndent()

    private const val MAIN_FUNCTION_NAME = "main"
    private const val GREET_FUNCTION_NAME = "greet"
    private const val NEW_FUNCTION_NAME = "newFunction"

    private val codeHintVar = """
      $greetFunction
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
          val secondUserAnswer: String = ""
          val thirdUserAnswer: String = ""
          println("End")
      }
    """.trimIndent()

    private val codeHintDoWhile = """
      $greetFunction
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            complete = isComplete(secret, guess)
        } while (!complete)
      }
    """.trimIndent()

    private val codeHintIf = """
      $greetFunction
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            } else if (isWon(complete, attempts, maxAttemptsCount)) {
                println("Congratulations! You guessed it!")
                break
            }
        } while (!complete)
      }
    """.trimIndent()

    private val codeHintFor = """
      $greetFunction
      fun main() {
        var newUserWord = ""
        for (i in secret.indices) {
            newUserWord += if (secret[i] == guess) {
                "secret[i]"
            } else {
                "currentUserWord[i * 2]"
            }
        }
      }
    """.trimIndent()

    private val codeHintWhen = """
      $greetFunction
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter) {
            "borders" -> applyBordersFilter(trimmedPicture)
            "squared" -> applySquaredFilter(trimmedPicture)
            else -> error("Unexpected filter")
        }
      }
    """.trimIndent()
  }
}
