package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.aiHints.core.log.Logger
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.actions.EduAIHintsUtils
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.ui.isDefault
import org.jetbrains.annotations.NonNls
import javax.swing.JButton
import javax.swing.JComponent

class AcceptHint : ApplyCodeAction() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isGetHintDiff()
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent = JButton(presentation.text).apply {
    isDefault = true
    isFocusable = true
    addActionListener {
      @Suppress("DEPRECATION") // BACKCOMPAT: 2024.2 Use [ActionUtil.invokeAction(AnAction, AnActionEvent, Runnable?)]
      ActionUtil.invokeAction(this@AcceptHint, ActionToolbar.getDataContextFor(this), place, null, null)
    }
  }

  override fun afterActionPerformed(project: Project) {
    EduAIHintsUtils.HintStateManager.getInstance(project).acceptHint()
    val task = project.getCurrentTask() ?: return
    TaskToolWindowView.getInstance(project).updateCheckPanel(task)
    EduAIFeaturesCounterUsageCollector.codeHintAccepted(task)
    Logger.aiHintsLogger.info(
      """|| Course id: ${task.course.id} | Lesson id: ${task.lesson.id} | Task id: ${task.id}
         || Action: code hint is accepted
      """.trimMargin()
    )
  }

  override fun showFailedNotification(project: Project) = EduNotificationManager.showErrorNotification(
    project,
    @Suppress("DialogTitleCapitalization") EduAIHintsCoreBundle.message("action.Educational.Hints.AcceptHint.notification.failed.title"),
    EduAIHintsCoreBundle.message("action.Educational.Hints.AcceptHint.notification.failed.text")
  )

  override fun getConfirmationFromDialog(project: Project): Boolean = true

  override val actionId: String = ACTION_ID

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Hints.AcceptHint"
  }
}