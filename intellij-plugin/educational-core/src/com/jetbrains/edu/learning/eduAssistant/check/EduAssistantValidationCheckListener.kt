package com.jetbrains.edu.learning.eduAssistant.check

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import kotlinx.coroutines.channels.Channel

class EduAssistantValidationCheckListener : CheckListener {
  private val finished = Channel<Boolean>(1)

  fun clear() {
    finished.tryReceive()
  }

  suspend fun wait() {
    finished.receive()
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    finished.trySend(true)
  }
}
