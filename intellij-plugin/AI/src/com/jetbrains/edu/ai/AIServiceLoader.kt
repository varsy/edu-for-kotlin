package com.jetbrains.edu.ai

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.jetbrains.edu.ai.translation.ui.AITranslationNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

abstract class AIServiceLoader(protected val project: Project, protected val scope: CoroutineScope) {
  protected val mutex = Mutex()

  val isRunning: Boolean
    get() = mutex.isLocked

  protected inline fun runInBackgroundExclusively(
    @NotificationContent lockNotAcquiredNotificationText: String,
    crossinline action: suspend () -> Unit
  ) {
    scope.launch {
      if (mutex.tryLock()) {
        try {
          action()
        }
        finally {
          mutex.unlock()
        }
      }
      else {
        AITranslationNotificationManager.showErrorNotification(project, message = lockNotAcquiredNotificationText)
      }
    }
  }
}