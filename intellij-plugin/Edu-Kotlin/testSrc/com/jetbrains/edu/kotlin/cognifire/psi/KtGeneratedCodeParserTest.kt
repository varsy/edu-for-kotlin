package com.jetbrains.edu.kotlin.cognifire.psi

import com.jetbrains.edu.cognifire.parsers.GeneratedCodeParser
import com.jetbrains.edu.cognifire.utils.RedundantTodoCleaner
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.cognifire.utils.toGeneratedCode
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse.GeneratedCodeLine
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtGeneratedCodeParserTest : EduTestCase() {

  @Test
  fun `test hasErrors without todo blocks`() {
    val generatedCode = """
      val picture = getPicture()
      val filter = chooseFilter()
      println("The transformed picture:")
      println(applyFilter(picture, filter))
    """.trimIndent()
    assertFalse(GeneratedCodeParser.hasErrors(project, generatedCode, mainFunctionSignature, language))
  }

  @Test
  fun `test hasErrors with todo block`() {
    val generatedCode = """
      if (secret == deleteSeparator(currentGuess)) {
          TODO("Specify what to do if the comparison is true")
      }
    """.trimIndent()

    assertTrue(GeneratedCodeParser.hasErrors(project, generatedCode, mainFunctionSignature, language))
  }

  @Test
  fun `test hasErrors with empty todo`() {
    val generatedCode = """
      val wordLength = 4
      val maxAttemptsCount = 3
      val secretExample = "ACEB"
      println(getGameRules(wordLength, maxAttemptsCount, secretExample))
      playGame(generateSecret(), wordLength, maxAttemptsCount)
      TODO()
    """.trimIndent()
    assertTrue(GeneratedCodeParser.hasErrors(project, generatedCode, mainFunctionSignature, language))
  }

  @Test
  fun `test hasErrors with todo messages related to return value`() {
    val promptToCodeTranslation = listOf(
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 0,
        promptLine = "Calculate the minimum of two values: the length of the string obtained by filtering characters in `secret` that are also in `guess`, and the length of the string obtained by filtering characters in `guess` that are also in `secret`.",
        generatedCodeLine = "val filteredSecret = secret.filter { it in guess }"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 1,
        promptLine = "Calculate the minimum of two values: the length of the string obtained by filtering characters in `secret` that are also in `guess`, and the length of the string obtained by filtering characters in `guess` that are also in `secret`.",
        generatedCodeLine = "val filteredGuess = guess.filter { it in secret }\n"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 2,
        promptLine = "Calculate the minimum of two values: the length of the string obtained by filtering characters in `secret` that are also in `guess`, and the length of the string obtained by filtering characters in `guess` that are also in `secret`.",
        generatedCodeLine = "val minLength = minOf(filteredSecret.length, filteredGuess.length)"
      ),
      GeneratedCodeLine(
        promptLineNumber = 0,
        codeLineNumber = 3,
        promptLine = "Calculate the minimum of two values: the length of the string obtained by filtering characters in `secret` that are also in `guess`, and the length of the string obtained by filtering characters in `guess` that are also in `secret`.",
        generatedCodeLine = "TODO(\"Specify the return value or further actions\")"
      ),
    )
    val promptToCode = RedundantTodoCleaner.deleteWrongTodo(promptToCodeTranslation, mainFunctionSignature)
    assertFalse(GeneratedCodeParser.hasErrors(project, promptToCode.toGeneratedCode(), mainFunctionSignature, language))
  }

  companion object {
    private val language = KotlinLanguage.INSTANCE
    private val mainFunctionSignature = FunctionSignature(
      name = "main",
      functionParameters = emptyList(),
      returnType = "Unit",
    )
  }
}
