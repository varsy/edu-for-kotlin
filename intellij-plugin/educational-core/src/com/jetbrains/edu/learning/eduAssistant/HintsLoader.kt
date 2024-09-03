package com.jetbrains.edu.learning.eduAssistant

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.learning.actions.ApplyCodeActionBase
import com.jetbrains.edu.learning.actions.NextStepHintAction.Companion.NEXT_STEP_HINT_DIFF_FLAG
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.eduAssistant.ui.HintInlineBanner
import com.jetbrains.edu.learning.eduAssistant.ui.HintInlineBanner.Companion.highlightFirstCodeDiffPositionOrNull
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.ml.hints.assistant.AiHintsAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class HintsLoader(private val project: Project, private val scope: CoroutineScope) {

  fun getHint(task: Task) = scope.launch(Dispatchers.IO) {
    try {
      if (!lock()) {
        showHintWindow(task, EduCoreBundle.message("action.Educational.NextStepHint.error.unlocked"))
        return@launch
      }

      val taskProcessor = TaskProcessorImpl(task)
      withBackgroundProgress(project, EduCoreBundle.message("action.Educational.NextStepHint.progress.short.text"), cancellable = true) {
        val assistantHint = AiHintsAssistant.getAssistant(taskProcessor).getHint().getOrElse {
          showHintWindow(task, it.message ?: EduCoreBundle.message("action.Educational.NextStepHint.error.unknown"))
          return@withBackgroundProgress
        }
        val codeHint = assistantHint.codeHint?.value
        if (codeHint != null) {
          val taskFile = taskProcessor.currentTaskFile ?: project.selectedTaskFile ?: error("Can't get task file")
          showHintWindow(task, assistantHint.textHint.value) {
            showInCodeAction(taskFile, codeHint)
          }
          return@withBackgroundProgress
        }
        showHintWindow(task, assistantHint.textHint.value)
      }
    }
    finally {
      unlock()
    }
  }

  private suspend fun showHintWindow(task: Task, textToShow: String, taskFile: TaskFile? = null, codeHint: String? = null, action: () -> Unit = {}) = withContext(Dispatchers.EDT) {
    val taskToolWindow = TaskToolWindowView.getInstance(project)
    val hintBanner = HintInlineBanner(project, textToShow, action)
    if (taskFile != null && codeHint != null) {
      val highlighter = highlightFirstCodeDiffPositionOrNull(project, taskFile, codeHint)
      hintBanner.apply {
        setCloseAction {
          highlighter?.dispose()
          taskToolWindow.removeInlineBannerFromCheckPanel(this)
        }
      }
    }
    task.status = CheckStatus.Unchecked
    taskToolWindow.updateCheckPanel(task)
    taskToolWindow.addInlineBannerToCheckPanel(hintBanner)
  }

  private fun showInCodeAction(taskFile: TaskFile, codeHint: String) {
    val virtualFile = taskFile.getVirtualFile(project) ?: error("VirtualFile for ${taskFile.name} not found")
    val solutionContent = DiffContentFactory.getInstance().create(VfsUtil.loadText(virtualFile), virtualFile.fileType)
    val solutionAfterChangesContent = DiffContentFactory.getInstance().create(codeHint, virtualFile.fileType)
    val request = SimpleDiffRequest(
      EduCoreBundle.message("action.Educational.NextStepHint.title"),
      solutionContent,
      solutionAfterChangesContent,
      EduCoreBundle.message("action.Educational.NextStepHint.current.solution"),
      EduCoreBundle.message("action.Educational.NextStepHint.solution.after.changes")
    )
    val diffRequestChain = SimpleDiffRequestChain(request)
    diffRequestChain.putUserData(ApplyCodeActionBase.VIRTUAL_FILE_PATH_LIST, listOf(virtualFile.path))
    diffRequestChain.putUserData(NEXT_STEP_HINT_DIFF_FLAG, true)
    DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
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