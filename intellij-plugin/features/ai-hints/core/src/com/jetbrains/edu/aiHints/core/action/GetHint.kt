package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbService
import com.jetbrains.edu.aiHints.core.HintsLoader
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.actions.EduActionUtils.GET_HINT_ACTION_ID
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class GetHint : ActionWithProgressIcon() {

  init {
    setUpSpinnerPanel(GET_HINT_ACTION_ID)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (DumbService.isDumb(project)) {
      e.dataContext.showPopup(ActionUtil.getUnavailableMessage(templateText, false))
      return
    }

    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return

    if (HintsLoader.isRunning(project)) {
      e.dataContext.showPopup(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.already.in.progress"))
      return
    }

    HintsLoader.getInstance(project).getHint(task)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}