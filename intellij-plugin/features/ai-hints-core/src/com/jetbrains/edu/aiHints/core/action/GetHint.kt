package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbService
import com.intellij.util.asSafely
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.core.HintStateManager
import com.jetbrains.edu.aiHints.core.HintsLoader
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.actions.EduAIHintsUtils.GET_HINT_ACTION_ID
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class GetHint : ActionWithProgressIcon() {

  init {
    setUpSpinnerPanel(GET_HINT_ACTION_ID)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    if (!isFeatureEnabled(EduExperimentalFeatures.AI_HINTS) || !UserAgreementSettings.getInstance().aiServiceAgreement) return
    val project = e.project ?: return
    val course = project.course.asSafely<EduCourse>() ?: return
    val task = project.getCurrentTask() ?: return
    val isMarketplaceStudyCourse = course.isMarketplaceRemote && course.isStudy
    val isFailedEduTask = task is EduTask && task.status == CheckStatus.Failed
    e.presentation.isVisible =
      isMarketplaceStudyCourse && isFailedEduTask && EduAIHintsProcessor.forCourse(course) != null
    e.presentation.isEnabled = HintStateManager.isDefault(project)
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

    val taskToolWindow = TaskToolWindowView.getInstance(project)
    val task = taskToolWindow.currentTask ?: return
    taskToolWindow.updateCheckPanel(task)
    EduAIFeaturesCounterUsageCollector.hintButtonClicked(task)
    HintsLoader.getInstance(project).getHint(task)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}