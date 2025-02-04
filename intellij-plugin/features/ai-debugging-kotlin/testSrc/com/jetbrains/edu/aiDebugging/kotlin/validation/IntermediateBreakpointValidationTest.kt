package com.jetbrains.edu.aiDebugging.kotlin.validation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.utils.editor.getVirtualFile
import com.jetbrains.edu.aiDebugging.core.breakpoint.IntermediateBreakpointProcessor
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.findTask
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test
import org.jetbrains.kotlinx.dataframe.*
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.*
import kotlin.io.path.exists
import java.nio.file.Paths

class IntermediateBreakpointValidationTest: EduTestCase() {

  @Test
  fun validate() {
    val path = object {}.javaClass.classLoader.getResource(DATASET)?.toURI()
                 ?.let { Paths.get(it) } ?: error("Failed to find dataset")
    if (!path.exists()) error("Path $path doesn't exist")
    val df = DataFrame.read(path.toFile())

    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val taskFile = task.taskFiles.values.first()

    val results = df.map { row ->
      val sourceCode = row["source_code_1"] as? String ?: error("Source code is empty")
      val wrongCodeLine = row["final_bp"] as? Int ?: error("Wrong code lines is empty")
      val expectedBPLineNumbers = (row["breakpoint_lines"] as? DataFrame<*>)?.map { it["value"] }?.toSet() ?: error("Wrong code lines is empty")
      val complexity = row["complexity"] as? String ?: error("Complexity is empty")

      replaceDocumentText(taskFile, project, sourceCode)
      val virtualFile = taskFile.getDocument(project)?.getVirtualFile() ?: error("Can't find virtual file for `${task.name}` task")

      val interBP = IntermediateBreakpointProcessor.calculateIntermediateBreakpointPositions(virtualFile, listOf(wrongCodeLine), project, language)
      val actualBPLines = (interBP.map { it + 1 } + wrongCodeLine).toSet()

      val tp = expectedBPLineNumbers.intersect(actualBPLines).size
      val fp = actualBPLines.subtract(expectedBPLineNumbers).size
      val fn = expectedBPLineNumbers.subtract(actualBPLines).size
      val interBpValidation = mutableMapOf<ClassificationResult, Int>().apply {
        put(ClassificationResult.TP, tp)
        put(ClassificationResult.FP, fp)
        put(ClassificationResult.FN, fn)
      }

      val numberOfLines = sourceCode.countCodeLinesSimple()

      ValidationResult(
        interBpValidation = interBpValidation,
        expectedBpDensity = expectedBPLineNumbers.count().toDouble() / numberOfLines,
        actualBpDensity = actualBPLines.count().toDouble() / numberOfLines,
        taskComplexity = complexity
      )
    }

    val finalBpResults = results.map { it.interBpValidation }
    val tp = finalBpResults.sumOf { it[ClassificationResult.TP] ?: 0 }
    val fp = finalBpResults.sumOf { it[ClassificationResult.FP] ?: 0 }
    val fn = finalBpResults.sumOf { it[ClassificationResult.FN] ?: 0 }
    val precision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
    val recall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
    val f1 = if (precision + recall > 0) 2 * (precision * recall) / (precision + recall) else 0.0

    val expectedBpDensityEasy = results.filter { it.taskComplexity == "Easy" }.map { it.expectedBpDensity }.average()
    val expectedBpDensityMiddle = results.filter { it.taskComplexity == "Middle" }.map { it.expectedBpDensity }.average()
    val expectedBpDensityHard = results.filter { it.taskComplexity == "Hard" }.map { it.expectedBpDensity }.average()
    val actualBpDensityEasy = results.filter { it.taskComplexity == "Easy" }.map { it.actualBpDensity }.average()
    val actualBpDensityMiddle = results.filter { it.taskComplexity == "Middle" }.map { it.actualBpDensity }.average()
    val actualBpDensityHard = results.filter { it.taskComplexity == "Hard" }.map { it.actualBpDensity }.average()

    println("""
      F1:          $f1
      Precision:   $precision
      Recall:      $recall
      Expected Bp Density for easy tasks: $expectedBpDensityEasy
      Actual Bp Density for easy tasks: $actualBpDensityEasy
      Expected Bp Density for middle tasks: $expectedBpDensityMiddle
      Actual Bp Density for middle tasks: $actualBpDensityMiddle
      Expected Bp Density for hard tasks: $expectedBpDensityHard
      Actual Bp Density for hard tasks: $actualBpDensityHard
      """)
  }

  private fun String.countCodeLinesSimple(): Int {
    var inMultilineComment = false
    return lines()
      .asSequence()
      .map { it.trim() }
      .filter { it.isNotEmpty() }
      .filter { line ->
        when {
          inMultilineComment -> {
            if (line.contains("*/")) inMultilineComment = false
            false
          }
          line.startsWith("/*") -> {
            inMultilineComment = !line.contains("*/")
            false
          }
          line.startsWith("//") -> false
          else -> true
        }
      }
      .count()
  }

  private fun replaceDocumentText(taskFile: TaskFile, project: Project, solution: String) {
    val currentDocument = taskFile.getDocument(project)
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        currentDocument?.setText(solution)
        currentDocument?.let { PsiDocumentManager.getInstance(project).commitDocument(it) }
        currentDocument?.let { FileDocumentManager.getInstance().saveDocument(it) }
      }
    }
  }

  data class ValidationResult(
    val interBpValidation: Map<ClassificationResult, Int>,
    val expectedBpDensity: Double,
    val actualBpDensity: Double,
    val taskComplexity: String
  )

  enum class ClassificationResult {
    TP, // True Positive
    FP, // False Positive
    FN, // False Negative
    TN  // True Negative
  }

  override fun createCourse() {
    StudyTaskManager.getInstance(project).course = courseWithFiles(courseMode = CourseMode.STUDENT) {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile(
            name = "Main.kt",
            text = """
            """.trimIndent()
          )
        }
      }
    }
  }

  companion object {
    private const val DATASET = "dataset/validation_dataset_manually_set_breakpoints.csv"
    private val language = KotlinLanguage.INSTANCE
  }
}