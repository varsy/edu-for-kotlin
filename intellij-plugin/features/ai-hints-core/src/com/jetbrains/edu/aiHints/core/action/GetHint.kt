package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbService
import com.jetbrains.edu.aiHints.core.EduAIHintsProvider
import com.jetbrains.edu.aiHints.core.HintsLoader
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.actions.EduActionUtils.GET_HINT_ACTION_ID
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.actions.EduActionUtils.isGetHintAvailable
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class GetHint : ActionWithProgressIcon() {

  init {
    setUpSpinnerPanel(GET_HINT_ACTION_ID)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = project.course ?: return
    val task = project.getCurrentTask() ?: return
    e.presentation.isEnabledAndVisible = isGetHintAvailable(task) && EduAIHintsProvider.forCourse(course) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (DumbService.isDumb(project)) {
      @Suppress("DEPRECATION") // BACKCOMPAT: 2024.2 Use [ActionUtil.getActionUnavailableMessage]
      return e.dataContext.showPopup(ActionUtil.getUnavailableMessage(templateText, false))
    }
    if (HintsLoader.isRunning(project)) {
      return e.dataContext.showPopup(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.already.in.progress"))
    }

    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    HintsLoader.getInstance(project).getHint(task)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}