package com.jetbrains.edu.ai.hints.validation.actions.next.step.hint

import com.jetbrains.edu.ai.hints.validation.accuracy.AccuracyCalculator
import com.jetbrains.edu.ai.hints.validation.actions.ValidationAction
import com.jetbrains.edu.ai.hints.validation.processor.ValidationHintProcessorImpl
import com.jetbrains.edu.ai.hints.validation.util.*
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.educational.ml.hints.assistant.AiHintsAssistant
import com.jetbrains.educational.ml.hints.processors.ValidationHintProcessor
import com.jetbrains.educational.ml.hints.validation.ValidationHintAssistant
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import kotlin.io.path.Path

/**
 * The `Auto Validate Compilation Error Hints Generation` action runs an automatic validation of educational AI assistant generating text and code hints when a compilation error occurs.
 * The output data can be found in `educational-plugin/aiAssistantValidation/generatedValidationOfCompilationErrorHints.csv` by default, validation
 * output directory can be set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     |   errorDetails  |  userCode  |  nextStepTextHint |  nextStepCodeHint  | errors | comprehensible | unnecessaryContent | hasExplanation | explanationCorrect | hasFix | fixCorrect | correctImplementation | improvementOverTheOriginal |
 * |:----------:|:-----------------:|:---------------:|:----------:|:-----------------:|:------------------:|:------:|:--------------:|:------------------:|:--------------:|:------------------:|:------:|:----------:|:---------------------:|:--------------------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       | package... | Replace the ...   | package ...        |   ...  |      Yes       |         No         |      BOH       |        Yes         |   No   |   Yes      |          Yes          |            No              |
 * |     ...    |        ...        |       ...       |     ...    |       ...         |       ...          |   ...  |      ...       |        ...         |       ...      |         ...        |   ...  |   ...      |          ...          |            ...             |
 * ```
 */
@Suppress("ComponentNotRegistered")
class AutoCompilationErrorHintValidationAction : ValidationAction<ValidationOfCompilationErrorHintsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedValidationOfCompilationErrorHints"
  override val progressText: String
    get() = EduAIBundle.message("action.Validation.AutoCompilationErrorHintValidation.progress.text")
  override val isNavigationRequired: Boolean = true
  override val pathToLabelledDataset by lazy { Path(System.getProperty("manual.hint.validation.path")) }
  override val accuracyCalculator = AutoCompilationErrorHintValidationAccuracyCalculator()

  init {
    setUpSpinnerPanel(progressText)
  }

  override fun MutableList<ValidationOfCompilationErrorHintsDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = ValidationOfCompilationErrorHintsDataframeRecord.buildFrom(this)

  private suspend fun buildRecord(
    validationProcessor: ValidationHintProcessor,
    taskId: Int,
    taskName: String
  ): ValidationOfCompilationErrorHintsDataframeRecord {
    val hintsValidation = ValidationHintAssistant(validationProcessor).processValidationCompilationErrorHints().getOrThrow()
    return ValidationOfCompilationErrorHintsDataframeRecord(
      taskId = taskId,
      taskName = taskName,
      errorDetails = validationProcessor.getErrorDetails(),
      userCode = validationProcessor.getUserCode(),
      nextStepTextHint = validationProcessor.getTextHint(),
      nextStepCodeHint = validationProcessor.getCodeHint(),
      comprehensible = hintsValidation.comprehensible,
      unnecessaryContent = hintsValidation.unnecessaryContent,
      hasExplanation = hintsValidation.hasExplanation,
      explanationCorrect = hintsValidation.explanationCorrect,
      hasFix = hintsValidation.hasFix,
      fixCorrect = hintsValidation.fixCorrect,
      correctImplementation = hintsValidation.correctImplementation,
      improvementOverTheOriginal = hintsValidation.improvementOverTheOriginal,
    )
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<ValidationOfCompilationErrorHintsDataframeRecord> {
    val taskProcessor = TaskProcessorImpl(task)
    val project = task.project ?: error("Cannot get project")
    runCheckAction(project)
    val response = AiHintsAssistant.getAssistant(taskProcessor).getHint()
    val assistantHint = response.getOrNull()

    return try {
      assistantHint ?: error("Cannot get assistant hint (${response.exceptionOrNull()?.message ?: "no assistant error found"})")
      listOf(buildRecord(
        ValidationHintProcessorImpl(project, taskProcessor, assistantHint),
        task.id,
        task.name
      ))
    } catch (e: Throwable) {
      listOf(
        ValidationOfCompilationErrorHintsDataframeRecord(
          taskId = task.id,
          taskName = task.name,
          errorDetails = taskProcessor.getTaskTextRepresentation(),
          userCode = project.selectedTaskFile?.getVirtualFile(project)?.getTextFromTaskTextFile() ?: "",
          nextStepCodeHint = assistantHint?.codeHint?.value ?: "",
          nextStepTextHint = assistantHint?.textHint?.value ?: "",
          errors = EduAIBundle.message("validation.error", e.message ?: "")
        )
      )
    }
  }

  override suspend fun buildRecords(manualValidationRecord: ValidationOfCompilationErrorHintsDataframeRecord): ValidationOfCompilationErrorHintsDataframeRecord =
    try {
      buildRecord(
        ValidationHintProcessorImpl(
          hintText = manualValidationRecord.nextStepTextHint,
          hintCode = manualValidationRecord.nextStepCodeHint,
          userCodeText = manualValidationRecord.userCode,
          detailsOfFailure = manualValidationRecord.errorDetails
        ),
        manualValidationRecord.taskId,
        manualValidationRecord.taskName
      )
    } catch (e: Throwable) {
      ValidationOfCompilationErrorHintsDataframeRecord(
        taskId = manualValidationRecord.taskId,
        taskName = manualValidationRecord.taskName,
        errorDetails = manualValidationRecord.errorDetails,
      )
    }

  inner class AutoCompilationErrorHintValidationAccuracyCalculator : AccuracyCalculator<ValidationOfCompilationErrorHintsDataframeRecord> {

    override fun calculateValidationAccuracy(
      manualRecords: List<ValidationOfCompilationErrorHintsDataframeRecord>,
      autoRecords: List<ValidationOfCompilationErrorHintsDataframeRecord>
    ) = ValidationOfCompilationErrorHintsDataframeRecord(
      nextStepCodeHint = ACCURACY_KEYWORD,
      comprehensible = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::comprehensible) { f, s ->
        areSameCriteria(f, s!!)
      },
      unnecessaryContent = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::unnecessaryContent) { f, s ->
        areSameCriteria(f, s!!)
      },
      hasExplanation = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::hasExplanation) { f, s ->
        areSameCriteria(f, s!!)
      },
      explanationCorrect = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::explanationCorrect) { f, s ->
        areSameCriteria(f, s!!)
      },
      hasFix = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::hasFix) { f, s ->
        areSameCriteria(f, s!!)
      },
      fixCorrect = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::fixCorrect) { f, s ->
        areSameCriteria(f, s!!)
      },
      correctImplementation = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::correctImplementation) { f, s ->
        areSameCriteria(f, s!!)
      },
      improvementOverTheOriginal = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::improvementOverTheOriginal) { f, s ->
        areSameCriteria(f, s!!)
      }
    )

    override fun calculateOverallAccuracy(records: List<ValidationOfCompilationErrorHintsDataframeRecord>) = ValidationOfCompilationErrorHintsDataframeRecord(
      nextStepCodeHint = ACCURACY_KEYWORD,
      comprehensible = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::comprehensible) { f -> isCorrectAnswer(f) },
      unnecessaryContent = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::unnecessaryContent) { f -> isCorrectAnswer(f, NO_KEYWORD) },
      hasExplanation = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::hasExplanation) { f -> isCorrectAnswer(f) },
      explanationCorrect = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::explanationCorrect) { f -> isCorrectAnswer(f) },
      hasFix = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::hasFix) { f -> isCorrectAnswer(f) },
      fixCorrect = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::fixCorrect) { f -> isCorrectAnswer(f) },
      correctImplementation = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::correctImplementation) { f -> isCorrectAnswer(f) },
      improvementOverTheOriginal = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::improvementOverTheOriginal) { f -> isCorrectAnswer(f) },
    )
  }
}
