package com.jetbrains.edu.assistant.validation.util

import org.apache.commons.csv.CSVRecord

data class CodeHintDataframeRecord(
  val taskId: Int,
  val taskName: String,
  val taskDescription: String,
  val codeHintPrompt: String?,
  val userCode: String,
  val errors: String? = null,
  val generatedCode: String? = null,
  val numberOfIssues: Int? = null,
  val issues: String? = null
) {
  constructor(
    taskId: Int,
    taskName: String,
    taskDescription: String,
    codeHintPrompt: String?,
    userCode: String,
    error: Throwable
  ) : this(
    taskId,
    taskName,
    taskDescription,
    codeHintPrompt,
    userCode,
    "Error while generating hint: ${error.message}",
  )

  companion object {
    fun buildFrom(record: CSVRecord) = CodeHintDataframeRecord(
      record.get(0).toInt(), record.get(1), record.get(2), record.get(3),
      record.get(4), record.get(5), record.get(6),
      record.get(7).toInt(), record.get(8))
  }
}

data class StudentSolutionRecord(
  val id: Int,
  val lessonName: String,
  val taskName: String,
  val code: String
) {
  companion object {
    fun buildFrom(record: CSVRecord) = StudentSolutionRecord(record.get(0).toInt(), record.get(1), record.get(2), record.get(3))
  }
}

data class ValidationOfHintsDataframeRecord(
  var taskId: Int = 0,
  var taskName: String = "",
  var taskDescription: String = "",
  var userCode: String = "",
  var nextStepTextHint: String = "",
  var nextStepCodeHint: String = "",
  var errors: String = "",
  var feedbackType : String = "",
  var information: String = "",
  var levelOfDetail: String = "",
  var personalized: String = "",
  var intersection: String = "",
  var appropriate: String = "",
  var specific: String = "",
  var misleadingInformation: String = "",
  var codeQuality: String = "",
  var kotlinStyle: String = "",
  var length: String = "",
) {
  companion object {
    fun buildFrom(record: CSVRecord) = ValidationOfHintsDataframeRecord(
      record.get(0).toInt(), record.get(1), record.get(2), record.get(3), record.get(4),
      record.get(5), record.get(6), record.get(7), record.get(8), record.get(9), record.get(10),
      record.get(11), record.get(12), record.get(13), record.get(14), record.get(15), record.get(16),
      record.get(17)
    )
  }
}

data class ValidationOfCompilationErrorHintsDataframeRecord(
  var taskId: Int = 0,
  var taskName: String = "",
  var errorDetails: String = "",
  var userCode: String = "",
  var nextStepTextHint: String = "",
  var nextStepCodeHint: String = "",
  var errors: String = "",
  var comprehensible: String = "",
  var unnecessaryContent: String = "",
  var hasExplanation: String = "",
  var explanationCorrect: String = "",
  var hasFix: String = "",
  var fixCorrect: String = "",
  var correctImplementation: String = "",
  var improvementOverTheOriginal: String = "",
) {
  companion object {
    fun buildFrom(record: CSVRecord) = ValidationOfCompilationErrorHintsDataframeRecord(
      record.get(0).toInt(), record.get(1), record.get(2), record.get(3), record.get(4),
      record.get(5), record.get(6), record.get(7), record.get(8), record.get(9), record.get(10),
      record.get(11), record.get(12), record.get(13), record.get(14)
    )
  }
}


data class MultipleCodeHintDataframeRecord(
  val hintIndex: Int,
  val taskId: Int,
  val taskName: String,
  val taskDescription: String,
  val codeHintPrompt: String? = null,
  val userCode: String?,
  val generatedCode: String?,
  val numberOfIssues: Int? = null,
  val issues: String? = null,
  val testStatus: String? = null,
  val errorMessage: String? = null
) {
  constructor(
    hintIndex: Int,
    taskId: Int,
    taskName: String,
    taskDescription: String,
    userCode: String,
    error: Throwable
  ) : this(
    hintIndex,
    taskId,
    taskName,
    taskDescription,
    userCode = userCode,
    generatedCode = "Error while generating hint: ${error.message}",
  )

  companion object {
    fun buildFrom(record: CSVRecord) = MultipleCodeHintDataframeRecord(
      record.get(0).toInt(), record.get(1).toInt(), record.get(2), record.get(3),
      record.get(4), record.get(5), record.get(6),
      record.get(7).toInt(), record.get(8), record.get(9)
    )
  }
}

data class MultipleCodeHintWithErrorDataframeRecord(
  val hintIndex: Int,
  val taskId: Int,
  val taskName: String,
  val taskDescription: String,
  val codeHintPrompt: String? = null,
  val userCode: String?,
  val generatedCode: String?,
  val testStatus: String? = null,
  val errorMessage: String? = null
) {
  constructor(
    hintIndex: Int,
    taskId: Int,
    taskName: String,
    taskDescription: String,
    userCode: String,
    error: Throwable
  ) : this(
    hintIndex,
    taskId,
    taskName,
    taskDescription,
    userCode = userCode,
    generatedCode = "Error while generating hint: ${error.message}",
  )

  companion object {
    fun buildFrom(record: CSVRecord) = MultipleCodeHintWithErrorDataframeRecord(
      record.get(0).toInt(), record.get(1).toInt(), record.get(2), record.get(3),
      record.get(4), record.get(5), record.get(8), record.get(9)
    )
  }
}
