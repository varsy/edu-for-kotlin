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
  private lateinit var aiAgreementCheckBox: Cell<JBCheckBox>

  private fun createInnerPanel(): JComponent = panel {
    row {
      text(EduCoreBundle.message("user.agreement.dialog.text"))
    }
    row {
      pluginAgreementCheckBox = checkBox(EMPTY_TEXT)
        .comment(EduCoreBundle.message("user.agreement.dialog.plugin.agreement.checkbox.comment"))
        .onChanged {
          if (!it.isSelected) {
            aiAgreementCheckBox.selected(false)
          }
          isOKActionEnabled = it.isSelected
        }
        .customize(UnscaledGaps.EMPTY)
      cell(createPluginAgreementCheckBoxTextPanel())
    }
    row {
      aiAgreementCheckBox = checkBox(EMPTY_TEXT)
        .enabledIf(pluginAgreementCheckBox.selected)
        .comment(EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.comment"))
        .customize(UnscaledGaps.EMPTY)
      cell(createAiAgreementCheckBoxTextPanel())
    }
  }

  @Suppress("DialogTitleCapitalization")
  private fun createPluginAgreementCheckBoxTextPanel(): JPanel = panel {
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

  @Suppress("DialogTitleCapitalization")
  private fun createAiAgreementCheckBoxTextPanel(): JPanel = panel {
    row {
      text(
        EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.text.first.row"),
        action = {
          EduBrowser.getInstance().browse(AI_TERMS_OF_USE_URL)
        })
        .customize(leftGap)
    }
    row {
      label(EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.text.second.row"))
        .customize(leftGap)
    }
    row {
      label(EduCoreBundle.message("user.agreement.dialog.ai.agreement.checkbox.text.third.row"))
        .customize(leftGap)
    }
  }

  fun showWithResult(): UserAgreementSettings.UserAgreementProperties {
    val result = showAndGet()
    if (!result) {
      return UserAgreementSettings.UserAgreementProperties(pluginAgreement = UserAgreementState.DECLINED)
    }

    val pluginAgreementState =
      if (pluginAgreementCheckBox.component.isSelected) UserAgreementState.ACCEPTED else UserAgreementState.DECLINED
    return UserAgreementSettings.UserAgreementProperties(pluginAgreement = pluginAgreementState)
  }

  companion object {
    private const val AI_TERMS_OF_USE_URL: String = "https://www.jetbrains.com/ai/terms-of-use/"
    private const val USER_AGREEMENT_URL: String = "https://www.jetbrains.com/legal/docs/terms/jetbrains-academy/plugin/"
    private const val PRIVACY_POLICY_URL: String = "https://www.jetbrains.com/legal/docs/privacy/privacy/"

    private const val EMPTY_TEXT: String = ""

    @RequiresEdt
    fun showUserAgreementDialog(project: Project?): Boolean {
      val result = UserAgreementDialog(project).showWithResult()
      userAgreementSettings().setUserAgreementSettings(result)
      val isAccepted = result.pluginAgreement == UserAgreementState.ACCEPTED
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