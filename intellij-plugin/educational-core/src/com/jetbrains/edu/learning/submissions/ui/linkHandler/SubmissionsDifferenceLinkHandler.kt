package com.jetbrains.edu.learning.submissions.ui.linkHandler

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.actions.ReportCommunitySolutionAction
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.*
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab.Companion.SUBMISSION_PROTOCOL
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab.Companion.textStyleHeader
import com.jetbrains.edu.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.ui.EduColors

class SubmissionsDifferenceLinkHandler(
  project: Project,
  private val task: Task,
  private val submissionsManager: SubmissionsManager,
  private val isCommunity: Boolean = false
) : SwingToolWindowLinkHandler(project) {

  override fun process(url: String, referUrl: String?): Boolean = with(url) {
    when {
      startsWith(SUBMISSION_DIFF_URL) -> {
        val submissionId = url.substringAfter(SUBMISSION_DIFF_URL).toInt()
        ApplicationManager.getApplication().executeOnPooledThread {
          val submission = submissionsManager.getSubmissionWithSolutionText(task, submissionId) ?: return@executeOnPooledThread
          runInEdt {
            showDiff(project, task, submission, isCommunity)
          }
        }
        return true
      }

      startsWith(SHOW_MORE_SOLUTIONS) -> {
        val taskId = task.id
        val communitySolutionsIds = submissionsManager.getCommunitySubmissionsFromMemory(taskId)?.mapNotNull { it.id }
        if (communitySolutionsIds.isNullOrEmpty()) {
          ApplicationManager.getApplication().executeOnPooledThread {
            submissionsManager.loadCommunitySubmissions(task)
          }
          return true
        }
        val latest = communitySolutionsIds.first()
        val oldest = communitySolutionsIds.last()
        ApplicationManager.getApplication().executeOnPooledThread {
          submissionsManager.loadMoreCommunitySubmissions(task, latest, oldest)
        }
        return true
      }
    }

    return false
  }

  private fun showDiff(project: Project, task: Task, submission: Submission, isCommunity: Boolean) {
    val taskFiles = task.taskFiles.values.toMutableList()
    val submissionTexts = submission.getSubmissionTexts(task.name) ?: return
    val submissionTaskFiles = taskFiles.filter { it.isVisible && !it.isTestFile }
    val submissionTaskFilePaths = mutableListOf<String>()
    val requests = submissionTaskFiles.mapNotNull {
      val virtualFile = it.getVirtualFile(project) ?: error("VirtualFile for ${it.name} not found")
      val documentText = FileDocumentManager.getInstance().getDocument(virtualFile)?.text
      val currentFileContent = if (documentText != null) DiffContentFactory.getInstance().create(documentText, virtualFile.fileType)
      else null
      val submissionText = submissionTexts[it.name] ?: submissionTexts[task.name]
      if (submissionText == null || currentFileContent == null) {
        null
      }
      else {
        submissionTaskFilePaths.add(virtualFile.path)
        val submissionFileContent = DiffContentFactory.getInstance().create(submissionText.removeAllTags(), virtualFile.fileType)
        createSimpleDiffRequest(currentFileContent, submissionFileContent, submission, isCommunity)
      }
    }
    val diffRequestChain = SimpleDiffRequestChain(requests)
    diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, submissionTaskFilePaths)
    if (project.isMarketplaceCourse() && isCommunity) {
      diffRequestChain.putCommunitySolution(task, submission)
      EduCounterUsageCollector.communitySolutionDiffOpened()
    }
    DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
  }

  private fun String.removeAllTags(): String = replace(OPEN_PLACEHOLDER_TAG.toRegex(), "").replace(CLOSE_PLACEHOLDER_TAG.toRegex(), "")

  private fun createSimpleDiffRequest(
    currentContent: DocumentContent, submissionContent: DocumentContent, submission: Submission, isCommunity: Boolean
  ): SimpleDiffRequest {
    val (title, title2) = if (!isCommunity) {
      EduCoreBundle.message("submissions.compare") to EduCoreBundle.message("submissions.submission")
    }
    else {
      val time = submission.time
      val formattedDate = time?.let { formatDate(time) } ?: ""
      EduCoreBundle.message("submissions.compare.community", formattedDate) to EduCoreBundle.message("submissions.community")
    }

    return SimpleDiffRequest(title, currentContent, submissionContent, EduCoreBundle.message("submissions.local"), title2)
  }

  private fun SimpleDiffRequestChain.putCommunitySolution(task: Task, submission: Submission) {
    putUserData(ReportCommunitySolutionAction.TASK_ID_KEY, task.id)
    putUserData(ReportCommunitySolutionAction.SUBMISSION_ID_KEY, submission.id)
  }

  companion object {
    private const val SUBMISSION_DIFF_URL = "${SUBMISSION_PROTOCOL}diff/"
    private const val SHOW_MORE_SOLUTIONS = "${SUBMISSION_PROTOCOL}more/"

    fun getSubmissionDiffLink(submissionId: Int?): String = "$SUBMISSION_DIFF_URL$submissionId"

    fun showMoreLink(): String =
      "<div style=\"padding-top:8px; padding-bottom:8px; padding-right:33px; text-align: center\">" +
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SHOW_MORE_SOLUTIONS>" +
      EduCoreBundle.message("submissions.tab.show.more.link") + "</a>" + "</div>"
  }
}