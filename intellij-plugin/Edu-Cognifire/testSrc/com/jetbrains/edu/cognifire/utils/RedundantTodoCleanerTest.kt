package com.jetbrains.edu.cognifire.utils

import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse.GeneratedCodeLine
import org.junit.Test

class RedundantTodoCleanerTest : EduTestCase() {

  private fun wrongTodo() = listOf(
    "//  TODO(   \"specify the return value here\"  )    ",
    "TODO(\"please specify the return value\")",
    "TODO(\"specify the return value here\")"
  )

  private fun normalTodo() = listOf(
    "TODO(\"add documentation\")",
    "TODO (\"  finish implementing the function  \")"
  )

  @Test
  fun `should delete TODO with 'specify the return value' at the end of code`() {
    wrongTodo().forEach { todo ->
      val promptToCodeTranslation = mutableListOf(
        GeneratedCodeLine(
          promptLineNumber = 0,
          codeLineNumber = 0,
          promptLine = "Create a variable named `name` and set it to the string \"John\".",
          generatedCodeLine = "val name = \"John\""
        ),
        GeneratedCodeLine(
          promptLineNumber = 0,
          codeLineNumber = 1,
          promptLine = "Create a variable named `name` and set it to the string \"John\".",
          generatedCodeLine = todo
        ),
      )
      val promptToCode = RedundantTodoCleaner.deleteWrongTodo(promptToCodeTranslation, mainFunctionSignature)
      promptToCodeTranslation.removeLast()
      assertEquals(promptToCodeTranslation, promptToCode)
    }
  }

  @Test
  fun `should not delete TODO with different message`() {
    normalTodo().forEach { todo ->
      val promptToCodeTranslation = mutableListOf(
        GeneratedCodeLine(
          promptLineNumber = 0,
          codeLineNumber = 0,
          promptLine = "Create a variable named `name` and set it to the string \"John\".",
          generatedCodeLine = "val name = \"John\""
        ),
        GeneratedCodeLine(
          promptLineNumber = 0,
          codeLineNumber = 1,
          promptLine = "Create a variable named `name` and set it to the string \"John\".",
          generatedCodeLine = todo
        ),
      )
      val promptToCode = RedundantTodoCleaner.deleteWrongTodo(promptToCodeTranslation, mainFunctionSignature)
      assertEquals(promptToCodeTranslation, promptToCode)
    }
  }

  @Test
  fun `should not delete TODO with 'specify the return value' if it's not the last line`() {
    wrongTodo().forEach { todo ->
      val promptToCodeTranslation = mutableListOf(
        GeneratedCodeLine(
          promptLineNumber = 0,
          codeLineNumber = 0,
          promptLine = "Create a variable named `name` and set it to the string \"John\".",
          generatedCodeLine = todo
        ),
        GeneratedCodeLine(
          promptLineNumber = 0,
          codeLineNumber = 1,
          promptLine = "Create a variable named `name` and set it to the string \"John\".",
          generatedCodeLine = "val name = \"John\""
        ),
      )
      val promptToCode = RedundantTodoCleaner.deleteWrongTodo(promptToCodeTranslation, mainFunctionSignature)
      assertEquals(promptToCodeTranslation, promptToCode)
    }
  }

  @Test
  fun `should not delete TODO with 'specify the return value' if function should return something`() {
    wrongTodo().forEach { todo ->
      val promptToCodeTranslation = mutableListOf(
        GeneratedCodeLine(
          promptLineNumber = 0,
          codeLineNumber = 0,
          promptLine = "Create a variable named `name` and set it to the string \"John\".",
          generatedCodeLine = "val name = \"John\""
        ),
        GeneratedCodeLine(
          promptLineNumber = 0,
          codeLineNumber = 1,
          promptLine = "Create a variable named `name` and set it to the string \"John\".",
          generatedCodeLine = todo
        ),
      )
      val functionSignature = FunctionSignature(
        name = "foo",
        functionParameters = emptyList(),
        returnType = "String",
      )
      val promptToCode = RedundantTodoCleaner.deleteWrongTodo(promptToCodeTranslation, functionSignature)
      assertEquals(promptToCodeTranslation, promptToCode)
    }
  }

  companion object {
    private val mainFunctionSignature = FunctionSignature(
      name = "main",
      functionParameters = emptyList(),
      returnType = "Unit",
    )
  }
}