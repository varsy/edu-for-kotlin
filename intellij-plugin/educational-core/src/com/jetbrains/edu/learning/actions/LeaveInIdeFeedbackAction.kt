package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.feedback.StudentInIdeFeedbackDialog
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import org.jetbrains.annotations.NonNls

class LeaveInIdeFeedbackAction :
  DumbAwareAction(EduCoreBundle.lazyMessage("action.leave.feedback.text"), EduCoreBundle.lazyMessage("action.leave.feedback.description")),
  RightAlignedToolbarAction
{

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return

    val dialog = StudentInIdeFeedbackDialog(project, task)
    dialog.showAndGet()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return
    project.getCurrentTask() ?: return
    if (project.course?.isMarketplace != true) return

    e.presentation.isEnabledAndVisible = true
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.EDT
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.LeaveInIdeFeedbackAction"

    fun promptToLeaveInIdeFeedback(project: Project) {
      val course = project.course ?: return
      if (course.lessons.count { it.isSolved } < 2) return
      val tasks = course.allTasks.filter { it !is TheoryTask }
      if (tasks.count { it.isSolved } / tasks.count().toDouble() < 30.00) return

      EduNotificationManager.showInfoNotification(project, "Leave Feedback Bro", "Would you like to leave feedback?")
    }
  }
}