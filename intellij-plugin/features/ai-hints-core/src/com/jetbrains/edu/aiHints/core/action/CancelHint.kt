package com.jetbrains.edu.aiHints.core.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.aiHints.core.log.Logger
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.ActionWithButtonCustomComponent
import com.jetbrains.edu.learning.actions.ApplyCodeAction.Companion.isGetHintDiff
import com.jetbrains.edu.learning.actions.EduActionUtils.closeFileEditor
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class CancelHint : ActionWithButtonCustomComponent(), DumbAware {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isGetHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    project.closeFileEditor(e)
    val task = project.getCurrentTask() ?: return
    TaskToolWindowView.getInstance(project).updateCheckPanel(task)
    EduAIFeaturesCounterUsageCollector.codeHintCancelled(task)
    Logger.aiHintsLogger.info(
      """|| Course id: ${task.course.id} | Lesson id: ${task.lesson.id} | Task id: ${task.id}
         || Action: code hint is cancelled
      """.trimMargin()
    )
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}