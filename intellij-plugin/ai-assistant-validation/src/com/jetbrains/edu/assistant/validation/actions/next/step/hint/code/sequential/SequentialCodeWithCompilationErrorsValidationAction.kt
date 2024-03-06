package com.jetbrains.edu.assistant.validation.actions.next.step.hint.code.sequential

import com.jetbrains.edu.assistant.validation.actions.next.step.hint.BaseAssistantInfoStorage
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.util.MultipleCodeHintWithErrorDataframeRecord
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.educational.ml.hints.assistant.AiAssistantHintInternal
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame

@Suppress("ComponentNotRegistered")
class SequentialCodeWithCompilationErrorsValidationAction : SequentialCodeValidationAction<MultipleCodeHintWithErrorDataframeRecord>() {
  override val outputFilePrefixName: String = "multipleCodeWithCompilationErrors"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.sequential.code.with.compilation.error.validation.action.name")
  override val isNavigationRequired: Boolean = true
  override val pathToLabelledDataset = null

  init {
    setUpSpinnerPanel(name)
  }

  override fun CSVRecord.toDataframeRecord() = MultipleCodeHintWithErrorDataframeRecord.buildFrom(this)

  override fun MutableList<MultipleCodeHintWithErrorDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun getRecord(
    task: EduTask,
    hintIndex: Int,
    baseAssistantInfoStorage: BaseAssistantInfoStorage,
    response: AiAssistantHintInternal?,
    userCode: String,
    currentUserCode: String
  ) = MultipleCodeHintWithErrorDataframeRecord(
    hintIndex = hintIndex,
    taskId = task.id,
    taskName = task.name,
    taskDescription = baseAssistantInfoStorage.taskProcessor.getTaskTextRepresentation(),
    codeHintPrompt = response?.codeHintPrompt?.value,
    userCode = userCode,
    generatedCode = response?.codeHint?.value,
    testStatus = task.status.name,
    errorMessage = task.feedback?.message
  )

  override fun getRecordWhenError(
    task: EduTask,
    hintIndex: Int,
    baseAssistantInfoStorage: BaseAssistantInfoStorage,
    response: AiAssistantHintInternal?,
    userCode: String,
    e: Throwable
  ) = MultipleCodeHintWithErrorDataframeRecord(
    hintIndex = hintIndex,
    taskId = task.id,
    taskName = task.name,
    taskDescription = baseAssistantInfoStorage.taskProcessor.getTaskTextRepresentation(),
    userCode = userCode,
    error = e
  )

  override fun needToBreak(task: EduTask) = task.status != CheckStatus.Unchecked && task.feedback?.message != EduCoreBundle.message("check.error.compilation.failed")
}