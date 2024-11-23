package com.jetbrains.edu.learning.submissions.ui.linkHandler

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.agreement.UserAgreementDialog
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab.Companion.SUBMISSION_PROTOCOL
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab.Companion.textStyleHeader
import com.jetbrains.edu.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.ui.EduColors

class LoginLinkHandler(
  project: Project,
  private val submissionsManager: SubmissionsManager
) : SwingToolWindowLinkHandler(project) {

  override fun process(url: String, referUrl: String?): Boolean = when {
    url.startsWith(SUBMISSION_LOGIN_URL) -> {
      submissionsManager.doAuthorize()
      true
    }

    url.startsWith(SUBMISSION_USER_AGREEMENT) -> {
      runInEdt { UserAgreementDialog.showUserAgreementDialog(project) }
      true
    }

    else -> false
  }

  companion object {
    private const val SUBMISSION_LOGIN_URL = "${SUBMISSION_PROTOCOL}login/"
    private const val SUBMISSION_USER_AGREEMENT = "${SUBMISSION_PROTOCOL}agreement/"

    fun getLoginText(): String = if (!RemoteEnvHelper.isRemoteDevServer()) {
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SUBMISSION_LOGIN_URL>" +
      EduCoreBundle.message("submissions.tab.login") + "</a>"
    }
    else {
      EduCoreBundle.message("submissions.wait.user.data.being.retrieved")
    }

    fun getAgreementPromptText(): String =
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SUBMISSION_USER_AGREEMENT>" +
      EduCoreBundle.message("submissions.tab.agreement") + "</a>"

    fun getSolutionSharingAgreementPromptText(): String =
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SUBMISSION_USER_AGREEMENT>" +
      EduCoreBundle.message("submissions.tab.solution.sharing.agreement") + "</a>"
  }
}
