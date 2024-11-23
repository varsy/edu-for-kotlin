package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.agreement.UserAgreementDialog
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils
import com.jetbrains.edu.learning.marketplace.SolutionSharingPromptCounter
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import java.util.concurrent.CompletableFuture
import javax.swing.JEditorPane

object SolutionSharingInlineBanners {

  fun promptToEnableSolutionSharing(project: Project, task: Task) {
    if (!SolutionSharingPromptCounter.shouldPrompt() || !task.supportSubmissions) return

    val inlineBanner = InlineBanner(EditorNotificationPanel.Status.Info).apply {
      setMessage(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.action.text"))
      addAction(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.prompt.description")) {
        CompletableFuture.supplyAsync {
          SubmissionsManager.getInstance(project).isSolutionSharingAllowed()
        }.thenApply { isSolutionSharingAllowed ->
          project.invokeLater {
            if (isSolutionSharingAllowed || UserAgreementDialog.showUserAgreementDialog(project)) {
              CompletableFuture.runAsync {
                MarketplaceSettings.INSTANCE.updateSharingPreference(true, project)
                EduCounterUsageCollector.solutionSharingInviteAction(true)
              }
              removeFromParent()
            }
          }
        }
      }
      setCloseAction {
        EduCounterUsageCollector.solutionSharingInviteAction(false)
      }
    }
    TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
    EduCounterUsageCollector.solutionSharingPromptShown()
    SolutionSharingPromptCounter.update()
  }

  fun showSuccessSolutionSharingEnabling(project: Project) {
    val inlineBanner = InlineBanner(EditorNotificationPanel.Status.Success).apply {
      // This is a local fix, which will also be available on the platform soon.
      val editorPane = UIUtil.findComponentOfType(this, JEditorPane::class.java)
      editorPane?.editorKit = HTMLEditorKitBuilder().build()
      setMessage(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.success.description"))
    }
    TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
  }

  fun showFailedToEnableSolutionSharing(project: Project?) {
    if (project == null) {
      MarketplaceNotificationUtils.showFailedToChangeSharingPreferenceNotification()
      return
    }

    val inlineBanner = InlineBanner(EditorNotificationPanel.Status.Error).apply {
      setMessage(EduCoreBundle.message("marketplace.solutions.sharing.inline.banner.failure.description"))
    }
    TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
  }
}
