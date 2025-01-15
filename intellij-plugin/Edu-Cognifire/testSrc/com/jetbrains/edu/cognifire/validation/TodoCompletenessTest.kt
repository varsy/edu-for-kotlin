package com.jetbrains.edu.cognifire.validation

import com.jetbrains.edu.cognifire.codegeneration.CodeGenerator
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.educational.ml.core.exception.AiAssistantException
import org.junit.Test
import java.nio.file.Files
import kotlin.io.path.exists
import java.nio.file.Paths
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import kotlinx.coroutines.*
import kotlinx.coroutines.awaitAll
import java.io.FileWriter
import java.time.Instant
import kotlin.io.path.Path
import kotlin.reflect.full.memberProperties

class TodoCompletenessTest: EduTestCase() {

  private val functionSignature = FunctionSignature("fakeName", emptyList(), "Unit")

  @Test
  fun testTodoCompleteness() = runBlocking {
    val manualValidationDataset = parseCsvFile()
    coroutineScope {
      val results = manualValidationDataset.map { record ->
        async { validateTodoPresence(record.prompt, record.sentencesIndicesWithTodo) }
      }
      val evaluatedResults = results.awaitAll().filterNotNull().flatten()
      val tp = evaluatedResults.count { it == ClassificationResult.TP }
      val fp = evaluatedResults.count { it == ClassificationResult.FP }
      val fn = evaluatedResults.count { it == ClassificationResult.FN }
      val precision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
      val recall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
      val f1 = if (precision + recall > 0) 2 * (precision * recall) / (precision + recall) else 0.0
      println("F1 = $f1")
      writeToCsvFile(f1.toString())
    }
  }

  private fun validateTodoPresence(promptText: String, sentencesIndicesWithTodo: List<Int>) =
    try {
      val promptExpression = PromptExpression(functionSignature, 0, 0, 0, promptText, "")
      val codeGenerator = CodeGenerator(promptExpression, project, FakeGradleBasedLanguage)
      val results = mutableListOf<ClassificationResult>()
      codeGenerator.promptToCodeLines.forEach { (promptIndex, _) ->
        val existsTodo = codeGenerator.finalPromptToCodeTranslation.filter { it.promptLineNumber == promptIndex }
          .map { it.generatedCodeLine }.any { it.contains(TODO) }
        val mustContainTodo = sentencesIndicesWithTodo.contains(promptIndex)
        val result = when {
          mustContainTodo && existsTodo -> ClassificationResult.TP
          mustContainTodo && !existsTodo -> ClassificationResult.FN
          !mustContainTodo && existsTodo -> ClassificationResult.FP
          else -> ClassificationResult.TN
        }
        results.add(result)
      }
      results
    } catch (e: AiAssistantException) {
      null
    } catch (_: Throwable) {
      null
    }

  private fun parseCsvFile(): List<PromptWithTodoDataframeRecord> {
    val path = object {}.javaClass.classLoader.getResource(PROMPTS_TABLE_NAME)?.toURI()
                 ?.let { Paths.get(it) } ?: error("Failed to find the prompt table")
    if (!path.exists()) error("Path $path doesn't exist")
    Files.newBufferedReader(path).use { reader ->
      return CsvMapper().readerFor(PromptWithTodoDataframeRecord::class.java)
        .with(CsvSchema.emptySchema().withHeader())
        .readValues<PromptWithTodoDataframeRecord>(reader)
        .readAll()
    }
  }

  private fun writeToCsvFile(todoCompleteness: String) {
    val headers = ValidationResultsDataframeRecord::class.memberProperties.map { it.name }
    var schema = CsvSchema.builder().setUseHeader(true).apply { headers.forEach { addColumn(it) } }.build()
    val file = Path(RESULT_TABLE_NAME).toFile()
    if (!file.exists()) {
      file.parentFile.mkdirs()
    } else {
      schema = schema.withoutHeader()
    }
    val newRow = ValidationResultsDataframeRecord(Instant.now().toString(), todoCompleteness)
    FileWriter(file, true).use { writer ->
      CsvMapper().writerFor(ValidationResultsDataframeRecord::class.java)
        .with(schema)
        .writeValues(writer)
        .write(newRow)
    }
  }

  companion object {
    private const val TODO = "TODO"
    private const val PROMPTS_TABLE_NAME = "dataset/prompts_with_todos.csv"
    private const val RESULT_TABLE_NAME = "dataset/validation_results.csv"
  }
}
