package com.jetbrains.edu.assistant.validation.processor

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.educational.ml.hints.assistant.AiAssistantHint
import com.jetbrains.educational.ml.hints.processors.ValidationHintProcessor

data class ValidationHintProcessorImpl(
  val hintText: String,
  val hintCode: String,
  val userCodeText: String,
  val description: String = "",
  val detailsOfFailure: String = "",
  val language: String = "",
) : ValidationHintProcessor {

  constructor(project: Project, taskProcessor: TaskProcessorImpl, assistantHint: AiAssistantHint) : this(
    hintText = assistantHint.textHint.value,
    hintCode = assistantHint.codeHint?.value ?: "",
    userCodeText = taskProcessor.currentTaskFile?.getVirtualFile(project)?.getTextFromTaskTextFile() ?: "",
    description = taskProcessor.getTaskTextRepresentation(),
    detailsOfFailure = taskProcessor.getTestFailureContext()?.details ?: "",
    language = taskProcessor.getLowercaseLanguageDisplayName()
  )

  override fun getTextHint() = hintText

  override fun getCodeHint(): String = hintCode

  override fun getErrorDetails() = detailsOfFailure

  override fun getLowercaseLanguageDisplayName() = language

  override fun getTaskDescription() = description

  override fun getUserCode() = userCodeText
}
