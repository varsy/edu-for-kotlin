package com.jetbrains.edu.agreement

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmissionsConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.submissions.isSubmissionDownloadAllowed
import javax.swing.JComponent
import javax.swing.JPanel

class UserAgreementDialog(project: Project?) : DialogWrapper(project) {
  private val leftGap = UnscaledGaps(0, 3, 0, 0)

  init {
    setOKButtonText(EduCoreBundle.message("user.agreement.dialog.agree.button"))
    isResizable = false
    isOKActionEnabled = false
    title = EduCoreBundle.message("user.agreement.dialog.title")
    init()
  }

  private var userAgreementSelected: Boolean = false
  private var statisticsSelected: Boolean = false

  override fun createCenterPanel(): JComponent = panel {
    row {
      icon(AllIcons.General.QuestionDialog).align(AlignY.TOP)
      cell(createInnerPanel())
    }
  }.apply { border = JBUI.Borders.empty(5) }

  private fun createInnerPanel(): JComponent = panel {
    row {
      text(EduCoreBundle.message("user.agreement.dialog.text"))
    }
    buttonsGroup {
      row {
        radioButton("I want to use JetBrains Academy Plugin with AI features and agree to the")
          .comment("Acceptance is required for use of the entire plugin")
          .onChanged {
            userAgreementSelected = it.isSelected
            isOKActionEnabled = isAnyCheckBoxSelected()
          }
          .customize(UnscaledGaps.EMPTY)
        cell(createCheckBoxTextPanel(withAiTermsOfService = true))
      }
      row {
        radioButton("I want to use JetBrains Academy Plugin without AI features and agree to the")
          .onChanged {
            statisticsSelected = it.isSelected
            isOKActionEnabled = isAnyCheckBoxSelected()
          }
          .customize(UnscaledGaps.EMPTY)
        cell(createCheckBoxTextPanel())
      }
    }
  }

  @Suppress("DialogTitleCapitalization")
  private fun createCheckBoxTextPanel(withAiTermsOfService: Boolean = false): JPanel = panel {
    row {
      link("JetBrains Academy Plugin User Agreement") { EduBrowser.getInstance().browse(USER_AGREEMENT_URL) }
        .resizableColumn()
        .customize(leftGap)
      if (!withAiTermsOfService) return@row

      label(EduCoreBundle.message("user.agreement.dialog.checkbox.and"))
        .customize(leftGap)
      link("JetBrains AI Terms of Service") { EduBrowser.getInstance().browse(PRIVACY_POLICY_URL) }
        .resizableColumn()
        .customize(leftGap)
    }
  }

  private fun isAnyCheckBoxSelected(): Boolean = userAgreementSelected || statisticsSelected

  fun showWithResult(): UserAgreementDialogResultState {
    val result = showAndGet()
    if (!result) {
      return UserAgreementDialogResultState(UserAgreementState.DECLINED, false)
    }

    val userAgreementState = if (userAgreementSelected) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED
    return UserAgreementDialogResultState(userAgreementState, statisticsSelected)
  }

  companion object {
    private const val USER_AGREEMENT_URL = "https://www.jetbrains.com/legal/docs/terms/jetbrains-academy/plugin/"
    private const val PRIVACY_POLICY_URL = "https://www.jetbrains.com/legal/docs/privacy/privacy/"

    @RequiresEdt
    fun showUserAgreementDialog(project: Project?): Boolean {
      val result = UserAgreementDialog(project).showWithResult()
      userAgreementSettings().setUserAgreementSettings(UserAgreementSettings.UserAgreementProperties(result.agreementState))
      val isAccepted = result.agreementState == UserAgreementState.ACCEPTED
      return isAccepted
    }

    @RequiresBackgroundThread
    fun showAtLogin() {
      if (!MarketplaceConnector.getInstance().isLoggedIn()) return
      val agreementState = MarketplaceSubmissionsConnector.getInstance().getUserAgreementState()
      if (!agreementState.isSubmissionDownloadAllowed()) {
        runInEdt {
          showUserAgreementDialog(null)
        }
      }
    }
  }
}

data class UserAgreementDialogResultState(val agreementState: UserAgreementState, val isStatisticsSharingAllowed: Boolean)