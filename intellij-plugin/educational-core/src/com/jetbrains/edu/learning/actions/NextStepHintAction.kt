package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.HintsLoader
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls

class NextStepHintAction : ActionWithProgressIcon(), DumbAware {

  init {
    setUpSpinnerPanel(EduCoreBundle.message("action.Educational.NextStepHint.progress.text"))
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (DumbService.isDumb(project)) {
      e.dataContext.showPopup(ActionUtil.getUnavailableMessage(EduCoreBundle.message("action.Educational.NextStepHint.title"), false))
      return
    }

    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return

    if (HintsLoader.isRunning(project)) {
      e.dataContext.showPopup(EduCoreBundle.message("action.Educational.NextStepHint.already.running"))
      return
    }

    HintsLoader.getInstance(project).getHint(task)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    fun isNextStepHintApplicable(task: Task) = task.course.id == 21067 && task is EduTask

    fun isAvailable(task: Task) = isNextStepHintApplicable(task) && task.course.courseMode == CourseMode.STUDENT && task.status == CheckStatus.Failed // TODO: when should we show this button?

    @NonNls
    const val ACTION_ID: String = "Educational.NextStepHint"

    val NEXT_STEP_HINT_DIFF_FLAG: Key<Boolean> = Key.create("nextStepHintDiffFlag")
  }
}
