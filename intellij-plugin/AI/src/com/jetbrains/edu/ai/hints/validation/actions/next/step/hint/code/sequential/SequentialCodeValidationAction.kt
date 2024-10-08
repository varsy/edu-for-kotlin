package com.jetbrains.edu.ai.hints.validation.actions.next.step.hint.code.sequential

import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.ai.hints.validation.actions.next.step.hint.BaseAssistantInfoStorage
import com.jetbrains.edu.ai.hints.validation.actions.next.step.hint.code.CodeValidationAction
import com.jetbrains.edu.ai.hints.validation.util.runCheckAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.assistant.AiAssistantHintInternal
import kotlinx.coroutines.delay

abstract class SequentialCodeValidationAction<T> : CodeValidationAction<T>() {

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<T> {
    return getCodeFromTaskFiles(task, lesson).map {
      runBlockingCancellable {
        buildCodeHintRecords(task, it, lesson)
      }
    }.flatten()
  }

  private fun Task.resetStatus() {
    status = CheckStatus.Unchecked
  }

  private suspend fun waitForProjectConfiguration(project: Project) {
    while (DumbService.isDumb(project)) {
      delay(50)
    }
  }

  abstract fun getRecord(
    task: EduTask,
    hintIndex: Int,
    baseAssistantInfoStorage: BaseAssistantInfoStorage,
    response: AiAssistantHintInternal?,
    userCode: String,
    currentUserCode: String
  ) : T

  abstract fun getRecordWhenError(
    task: EduTask,
    hintIndex: Int,
    baseAssistantInfoStorage: BaseAssistantInfoStorage,
    response: AiAssistantHintInternal?,
    userCode: String,
    e: Throwable
  ) : T

  abstract fun needToBreak(task: EduTask): Boolean

  private suspend fun buildCodeHintRecords(
    task: EduTask,
    userCode: String,
    lesson: Lesson,
  ): List<T> {
    val baseAssistantInfoStorage = BaseAssistantInfoStorage(task)
    waitForProjectConfiguration(baseAssistantInfoStorage.project)

    // To avoid saving old task states
    task.resetStatus()
    runCheckAction(baseAssistantInfoStorage.project)

    val records = mutableListOf<T>()
    var currentUserCode = userCode

    for (hintIndex in 1..MAX_HINTS) {
      var response: AiAssistantHintInternal? = null
      try {
        runBlockingCancellable {
          withBackgroundProgress(baseAssistantInfoStorage.project, GETTING_HINT_MESSAGE, false) {
            response = baseAssistantInfoStorage.assistant.getHintInternal(currentUserCode).getOrNull()
          }
        }

        currentUserCode = response?.codeHint?.value ?: error("Code hint is empty")

        lesson.replaceContent(task, currentUserCode, baseAssistantInfoStorage.project)

        runCheckAction(baseAssistantInfoStorage.project)

        records.add(getRecord(task, hintIndex, baseAssistantInfoStorage, response, userCode, currentUserCode))

        if (needToBreak(task)) {
          break
        }
      }
      catch (e: Throwable) {
        records.add(getRecordWhenError(task, hintIndex, baseAssistantInfoStorage, response, userCode, e))
      }
    }

    lesson.replaceContent(task, currentUserCode, baseAssistantInfoStorage.project)
    return records
  }
  companion object {
    private const val GETTING_HINT_MESSAGE = "Getting Hint"
    private const val MAX_HINTS = 10
  }
}
