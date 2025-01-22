package com.jetbrains.edu.cognifire.log

import com.intellij.idea.LoggerFactory
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.lazyPub
import com.jetbrains.edu.cognifire.grammar.UnparsableSentenceLink
import com.jetbrains.edu.cognifire.manager.PromptCodeState
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object Logger {
  private val default: Logger = LoggerFactory().getLoggerInstance("Cognifire")

  val cognifireLogger: Logger by lazyPub {
    BaseCognifireLoggerFactory("Cognifire").getLoggerInstanceOrNull() ?: default
  }

  fun getLoggerInfo(
    task: Task,
    prodeId: String,
    newPromptExpression: PromptExpression,
    generatedCode: String,
    state: PromptCodeState,
    unparsableSentenceLink: UnparsableSentenceLink,
  ) =
    """Lesson id: ${task.lesson.id}    Task id: ${task.id}    Action id: $prodeId
     | Text prompt: ${newPromptExpression.prompt}
     | Code prompt: ${newPromptExpression.code}
     | Generated code: $generatedCode
     | Has TODO blocks: ${state == PromptCodeState.CodeFailed}
     | Unparsable sentences: $unparsableSentenceLink
  """.trimMargin()

  fun getLoggerInfo(
    task: Task,
    prodeId: String,
    title: String,
    errorMessage: String,
    promptExpression: PromptExpression
  ) =
    """Lesson id: ${task.lesson.id}    Task id: ${task.id}    Action id: $prodeId
     | Error: $title
     | ErrorMessage: $errorMessage
     | Text prompt: ${promptExpression.prompt}
     | Code prompt: ${promptExpression.code}
    """.trimMargin()

  fun getLoggerInfo(
    task: Task,
    prodeId: String,
    title: String,
    errorMessage: String
  ) =
    """Lesson id: ${task.lesson.id}    Task id: ${task.id}    Action id: $prodeId
     | Error: $title
     | ErrorMessage: $errorMessage
    """.trimMargin()
}
