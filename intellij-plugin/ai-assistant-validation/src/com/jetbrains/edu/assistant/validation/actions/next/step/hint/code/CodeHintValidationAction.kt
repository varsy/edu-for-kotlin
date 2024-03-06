package com.jetbrains.edu.assistant.validation.actions.next.step.hint.code

import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.assistant.validation.actions.next.step.hint.BaseAssistantInfoStorage
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.util.CodeHintDataframeRecord
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame

/**
 * The `Validate Code Hint Generation` action prepares a dataset for manual validation of educational AI assistant generating code hints.
 * The output data can be found in `educational-plugin/aiAssistantValidation/codeHints.csv` by default, validation output directory can be
 * set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     | taskDescription | taskAnalysisPrompt |                             steps                            | codeHintPrompt |            userCode            | errors |    generatedCode    | numberOfIssues | issues |
 * |:----------:|:-----------------:|:---------------:|:------------------:|:------------------------------------------------------------:|:--------------:|:------------------------------:|:------:|:-------------------:|:--------------:|:------:|
 * | 1412191977 | ProgramEntryPoint |       ...       |         ...        | 1. Replace the existing output text with the string "Hello!" |       ...      | println(""My first program!"") |   ...  | println(""Hello!"") |        0       |        |
 * |     ...    |        ...        |       ...       |         ...        |                              ...                             |       ...      |               ...              |   ...  |         ...         |       ...      |   ...  |
 * ```
 */
@Suppress("ComponentNotRegistered")
class CodeHintValidationAction : CodeValidationAction<CodeHintDataframeRecord>() {

  override val outputFilePrefixName: String = "codeHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.code.hint.validation.action.name")
  override val isNavigationRequired: Boolean = true
  override val pathToLabelledDataset = null

  init {
    setUpSpinnerPanel(name)
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<CodeHintDataframeRecord> {
    return getCodeFromTaskFiles(task, lesson).map {
      runBlockingCancellable {
        buildCodeHintRecord(task, it)
      }
    }
  }

  private suspend fun buildCodeHintRecord(task: EduTask, userCode: String): CodeHintDataframeRecord {
    val baseAssistantInfoStorage = BaseAssistantInfoStorage(task)
    val response = baseAssistantInfoStorage.assistant.getHintInternal(userCode).getOrNull()

    try {
      val issues = runInspections(baseAssistantInfoStorage.project, baseAssistantInfoStorage.language, userCode)

      return CodeHintDataframeRecord(
        taskId = task.id,
        taskName = task.name,
        taskDescription = baseAssistantInfoStorage.taskProcessor.getTaskTextRepresentation(),
        codeHintPrompt = response?.codeHintPrompt?.value,
        userCode = userCode,
        generatedCode = response?.codeHint?.value ?: "",
        numberOfIssues = issues.size,
        issues = issues.joinToString(",")
      )
    }
    catch (e: Throwable) {
      return CodeHintDataframeRecord(
        taskId = task.id,
        taskName = task.name,
        taskDescription = baseAssistantInfoStorage.taskProcessor.getTaskTextRepresentation(),
        codeHintPrompt = response?.codeHintPrompt?.value,
        userCode = userCode,
        error = e
      )
    }
  }

  override fun MutableList<CodeHintDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = CodeHintDataframeRecord.buildFrom(this)
}
