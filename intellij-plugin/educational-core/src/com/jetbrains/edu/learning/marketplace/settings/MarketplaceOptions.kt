package com.jetbrains.edu.learning.marketplace.settings

import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.agreement.userAgreementSettings
import com.jetbrains.edu.learning.authUtils.EduLoginConnector
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT_PROFILE_PATH
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.settings.OAuthLoginOptions
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.isAccepted
import javax.swing.JComponent

class MarketplaceOptions : OAuthLoginOptions<MarketplaceAccount>() {

  override val connector: EduLoginConnector<MarketplaceAccount, *>
    get() = MarketplaceConnector.getInstance()

  override fun isAvailable(): Boolean = true

  override fun getDisplayName(): String = JET_BRAINS_ACCOUNT

  override fun profileUrl(account: MarketplaceAccount): String = JET_BRAINS_ACCOUNT_PROFILE_PATH

  override fun getLogoutText(): String = ""

  override fun createLogOutListener(): HyperlinkAdapter? = null

  override fun postLoginActions() {
    super.postLoginActions()
    val openProjects = ProjectManager.getInstance().openProjects
    openProjects.forEach {
      if (!it.isDisposed && it.course is EduCourse) SubmissionsManager.getInstance(it).prepareSubmissionsContentWhenLoggedIn()
    }
    // todo: enable options
  }

  private val userAgreementSettings = userAgreementSettings()
  private var submissionsServiceCheckBoxSelected = userAgreementSettings.userAgreementProperties.value.submissionsServiceAgreement.isAccepted()
  private var aiServiceCheckBoxSelected = userAgreementSettings.userAgreementProperties.value.aiServiceAgreement.isAccepted()

  override fun getAdditionalComponents(): List<JComponent> = listOf(
    panel {
      if (!RemoteEnvHelper.isRemoteDevServer()) {
        row {
          checkBox(EduCoreBundle.message("marketplace.options.user.agreement.checkbox"))
            .bindSelected(::submissionsServiceCheckBoxSelected)
        }
      }
      row {
        checkBox("Solution Sharing")
      }
      row {
        checkBox(EduCoreBundle.message("marketplace.options.ai.service.checkbox"))
          .bindSelected(::aiServiceCheckBoxSelected)
      }
    }
  )

  override fun apply() {
    super.apply()
    // todo
  }

  override fun reset() {
    super.reset()
    // todo()
  }

  override fun isModified(): Boolean {
    return super.isModified() // || todo
  }
}