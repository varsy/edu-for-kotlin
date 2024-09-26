package com.jetbrains.edu.learning.marketplace

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.LeaveInIdeFeedbackAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showSubmissionNotPostedNotification
import com.jetbrains.edu.learning.marketplace.actions.PostMarketplaceProjectToGitHub
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.SolutionSharingInlineBanners
import java.util.concurrent.CompletableFuture

class MarketplaceCheckListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course as? EduCourse ?: return
    if (!course.isStudy) return

    val submissionsManager = SubmissionsManager.getInstance(project)
    course.postSubmission(project, task, result, submissionsManager)
    submissionsManager.loadCommunitySubmissions(task, result)
    EduCounterUsageCollector.submissionSuccess(result.isSolved)

    if (result.isSolved) {
      PostMarketplaceProjectToGitHub.promptToPostProject(project)
      SolutionSharingInlineBanners.promptToEnableSolutionSharing(project, task)
      LeaveInIdeFeedbackAction.promptToLeaveInIdeFeedback(project)
    }
  }

  /**
   * Posts the submission to the Submissions Server if the course is published on the Marketplace and if the task can be submitted.
   * Submission posting is performed only when the course version matches the latest version available on the Marketplace.
   */
  private fun EduCourse.postSubmission(project: Project, task: Task, result: CheckResult, submissionsManager: SubmissionsManager) {
    if (!isMarketplaceRemote || !task.isToSubmitToRemote) return

    MarketplaceConnector.getInstance().isLoggedInAsync().thenApplyAsync { isLoggedIn ->
      when {
        !isLoggedIn -> return@thenApplyAsync

        isUpToDate -> {
          val submission = MarketplaceSubmissionsConnector.getInstance().postSubmission(project, task, result)
          submissionsManager.addToSubmissionsWithStatus(task.id, task.status, submission)
        }

        else -> {
          showSubmissionNotPostedNotification(project, this, task.name)
        }
      }
    }
  }

  private fun SubmissionsManager.loadCommunitySubmissions(task: Task, result: CheckResult) {
    if (!task.supportSubmissions || !result.isSolved) return

    CompletableFuture.runAsync({
      loadCommunitySubmissions(task)
    }, ProcessIOExecutorService.INSTANCE)
  }
}