package com.jetbrains.edu.learning.eduAssistant

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.eduAssistant.ui.HintsBannerManager
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.educational.ml.hints.assistant.AiHintsAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class HintsLoader(private val project: Project, private val scope: CoroutineScope) {

  fun getHint(task: Task) = scope.launch(Dispatchers.IO) {
    try {
      if (!lock()) {
        HintsBannerManager.showTextHintBanner(project, task, EduCoreBundle.message("action.Educational.NextStepHint.error.unlocked"))
        return@launch
      }

      val taskProcessor = TaskProcessorImpl(task)
      val hint = withBackgroundProgress(project, EduCoreBundle.message("action.Educational.NextStepHint.progress.short.text"), cancellable = true) {
        AiHintsAssistant.getAssistant(taskProcessor).getHint()
      }.getOrElse {
        HintsBannerManager.showTextHintBanner(project, task, it.message ?: EduCoreBundle.message("action.Educational.NextStepHint.error.unknown"))
        return@launch
      }

      val codeHint = hint.codeHint?.value
      if (codeHint != null) {
        val taskFile = taskProcessor.currentTaskFile ?: project.selectedTaskFile ?: error("Failed to obtain TaskFile")
        return@launch HintsBannerManager.showCodeHintBanner(project, task, taskFile, hint.textHint.value, codeHint)
      }

      HintsBannerManager.showTextHintBanner(project, task, hint.textHint.value)
    }
    finally {
      unlock()
    }
  }

  private val lock = AtomicBoolean(false)

  private val isLocked: Boolean
    get() = lock.get()

  private fun lock(): Boolean {
    return lock.compareAndSet(false, true)
  }

  private fun unlock() {
    lock.set(false)
  }

  companion object {
    fun getInstance(project: Project): HintsLoader = project.service()
    fun isRunning(project: Project): Boolean = getInstance(project).isLocked
  }
}