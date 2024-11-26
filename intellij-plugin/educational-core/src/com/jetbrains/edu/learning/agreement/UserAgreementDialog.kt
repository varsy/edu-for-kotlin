package com.jetbrains.edu.learning.agreement

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.Companion.userAgreementSettings
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

  override fun createCenterPanel(): JComponent = panel {
    row {
      icon(AllIcons.General.QuestionDialog).align(AlignY.TOP)
      cell(createInnerPanel())
    }
  }.apply { border = JBUI.Borders.empty(5) }

  private lateinit var pluginAgreementCheckBox: Cell<JBCheckBox>
  private lateinit var aiTermsOfServiceCheckBox: Cell<JBCheckBox>
  private var userAgreementSelected: Boolean = false
  private var statisticsSelected: Boolean = false

  private fun createInnerPanel(): JComponent = panel {
    row {
      text(EduCoreBundle.message("user.agreement.dialog.text"))
    }
    row {
      pluginAgreementCheckBox = checkBox("")
        .comment(EduCoreBundle.message("user.agreement.dialog.agreement.checkbox.comment"))
        .onChanged {
          userAgreementSelected = it.isSelected
          if (!it.isSelected) {
            aiTermsOfServiceCheckBox.selected(false)
          }
          isOKActionEnabled = it.isSelected
        }
        .customize(UnscaledGaps.EMPTY)
      cell(createCheckBoxTextPanel())
    }
    row {
      aiTermsOfServiceCheckBox = checkBox("")
        .enabledIf(pluginAgreementCheckBox.selected)
        .customize(leftGap)
      text("<a>JetBrains AI Terms of Service</a>. I hereby grant JetBrains consent to use my inputs and data to improve JetBrains AI, including for training JetBrains' machine learning models", action = {
        EduBrowser.getInstance().browse(AI_TERMS_OF_USE_URL)
      })
    }
  }

  @Suppress("DialogTitleCapitalization")
  private fun createCheckBoxTextPanel(): JPanel = panel {
    row {
      link(EduCoreBundle.message("user.agreement.dialog.checkbox.agreement")) { EduBrowser.getInstance().browse(USER_AGREEMENT_URL) }
        .resizableColumn()
        .customize(leftGap)
      label(EduCoreBundle.message("user.agreement.dialog.checkbox.and"))
        .customize(leftGap)
      link(EduCoreBundle.message("user.agreement.dialog.checkbox.privacy.policy")) { EduBrowser.getInstance().browse(PRIVACY_POLICY_URL) }
        .resizableColumn()
        .customize(leftGap)
    }
  }

  fun showWithResult(): UserAgreementDialogResultState {
    val result = showAndGet()
    if (!result) {
      return UserAgreementDialogResultState(UserAgreementState.DECLINED, false)
    }

    val userAgreementState = if (userAgreementSelected) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED
    return UserAgreementDialogResultState(userAgreementState, statisticsSelected)
  }

  companion object {
    private const val AI_TERMS_OF_USE_URL = "https://www.jetbrains.com/ai/terms-of-use/"
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