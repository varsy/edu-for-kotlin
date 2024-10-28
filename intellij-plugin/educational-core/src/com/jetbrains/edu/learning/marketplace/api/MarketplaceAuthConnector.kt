package com.jetbrains.edu.learning.marketplace.api

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.JBAccountInfoService
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.authUtils.EduLoginConnector
import com.jetbrains.edu.learning.authUtils.OAuthUtils.GrantType.JBA_TOKEN_EXCHANGE
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.marketplace.HUB_AUTH_URL
import com.jetbrains.edu.learning.marketplace.JET_BRAINS_ACCOUNT
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showInstallMarketplacePluginNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showReloginToJBANeededNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceOAuthBundle
import com.jetbrains.edu.learning.marketplace.userAgreement.UserAgreementDialog
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.network.executeHandlingExceptions
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.util.*
import java.util.concurrent.TimeUnit

abstract class MarketplaceAuthConnector : EduLoginConnector<MarketplaceAccount, JBAccountUserInfo>() {
  override val clientSecret: String = MarketplaceOAuthBundle.value("eduHubClientSecret")

  override val clientId: String = MarketplaceOAuthBundle.value("eduHubClientId")

  override fun doAuthorize(vararg postLoginActions: Runnable, authorizationPlace: AuthorizationPlace) {
    if (RemoteEnvHelper.isRemoteDevServer()) return
    this.authorizationPlace = authorizationPlace
    ApplicationManager.getApplication().executeOnPooledThread {
      login(
        *postLoginActions,
        Runnable {
          runInBackground(null, EduCoreBundle.message("user.agreement.getting.state"), false) { UserAgreementDialog.showAtLogin() }
        })
    }
  }

  fun setPostLoginActions(vararg postLoginActions: Runnable): List<Runnable> {
    val requestFocus = Runnable { runInEdt { requestFocus() } }
    val showNotification = Runnable {
      val userName = account?.userInfo?.getFullName() ?: return@Runnable
      EduNotificationManager.showInfoNotification(
        title = EduCoreBundle.message("login.successful"),
        content = EduCoreBundle.message("logged.in.as", userName)
      )
    }
    return postLoginActions.asList() + listOf(requestFocus, showNotification)
  }

  override fun isLoggedIn(): Boolean {
    if (RemoteEnvHelper.isRemoteDevServer()) {
      return RemoteEnvHelper.getUserUidToken() != null
    }
    if (account == null) return false
    val jbaInfoService = getJBAccountInfoServiceWithNotification()
    return jbaInfoService.isLoggedIn()
  }

  private fun login(vararg postLoginActions: Runnable) {
    val jbAccountInfoService = getJBAccountInfoServiceWithNotification() ?: return

    val currentAccount = account
    if (jbAccountInfoService.userData == null) {
      invokeJBALogin(jbAccountInfoService, setPostLoginActions(*postLoginActions))
      return
    }
    else if (currentAccount == null || !jbAccountInfoService.isLoggedIn()) {
      LOG.info("JB access token not available. Relogin needed to proceed")
      showReloginToJBANeededNotification(invokeJBALoginAction(jbAccountInfoService, *postLoginActions))
    }
  }

  private fun JBAccountInfoService?.isLoggedIn(): Boolean = this?.userData != null

  private fun getJBAccountInfoServiceWithNotification(): JBAccountInfoService? {
    val jbAuthService = JBAccountInfoService.getInstance()
    return if (jbAuthService == null) {
      LOG.warn("JBAccountInfoService is null")
      showInstallMarketplacePluginNotification()
      null
    }
    else {
      jbAuthService
    }
  }

  fun invokeJBALogin(jbAuthService: JBAccountInfoService, postLoginActions: List<Runnable>) {
    jbAuthService.invokeJBALogin({postLoginActions.forEach { it.run() }}, {
      EduNotificationManager.showErrorNotification(
        title = EduCoreBundle.message("error.login.failed"),
        content = EduCoreBundle.message("error.failed.login.to.subsystem", JET_BRAINS_ACCOUNT)
      )
    })
  }

  /*
  Possible return values:
    - null means no jba token in IDE and you need to relogin (possible when user logged in from toolbox or when token refresh inside IDE failed)
    - exception means some network error
    - non-null value - correct hab token
   */
  @RequiresBackgroundThread
  fun loadHubToken(): String? {
    val jbAccountInfoService = getJBAccountInfoServiceWithNotification() ?: return null
    val jbAccessToken = getJBAccessToken(jbAccountInfoService) ?: return null

    return retrieveHubToken(jbAccessToken).accessToken
  }

  @RequiresBackgroundThread
  fun getJBAccessToken(jbAccountInfoService: JBAccountInfoService): String? {
    var success = false
    return try {
      val jbAccessToken = jbAccountInfoService.accessToken.get(30, TimeUnit.SECONDS)
      success = jbAccessToken != null
      jbAccessToken
    }
    catch (e: Exception) {
      LOG.warn(e)
      null
    }
    finally {
      if (GetJBATokenSuccessRecorder.getInstance().updateState(success)) {
        EduCounterUsageCollector.obtainJBAToken(success)
      }
    }
  }

  fun invokeJBALoginAction(jbAuthService: JBAccountInfoService, vararg postLoginActions: Runnable) = object : AnAction(EduCoreBundle.message("action.relogin.to.jba")) {
    override fun actionPerformed(e: AnActionEvent) {
      LOG.warn("Invoking login to JB account")
      invokeJBALogin(jbAuthService, setPostLoginActions(*postLoginActions))
    }
  }

  private fun retrieveHubToken(jbaAccessToken: String): TokenInfo {
    val response = extensionGrantsEndpoint.exchangeTokens(JBA_TOKEN_EXCHANGE, jbaAccessToken,
                                                          MARKETPLACE_CLIENT_ID).executeHandlingExceptions()
    return response?.body() ?: error("Failed to obtain hub token via extension grants. JetBrains account access token is null")
  }

  private val extensionGrantsEndpoint: HubExtensionGrantsEndpoints
    get() = getHubExtensionGrantsEndpoints()

  private fun getHubExtensionGrantsEndpoints(): HubExtensionGrantsEndpoints {
    val retrofit = createRetrofitBuilder(
      HUB_AUTH_URL, connectionPool,
      accessToken = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray()),
      authHeaderValue = AUTH_TYPE_BASIC)
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(HubExtensionGrantsEndpoints::class.java)
  }

  override fun getFreshAccessToken(userAccount: MarketplaceAccount?, accessToken: String?): String? {
    return accessToken
  }

  companion object {
    private val LOG = logger<MarketplaceAuthConnector>()
    private val MARKETPLACE_CLIENT_ID: String = MarketplaceOAuthBundle.value("marketplaceHubClientId")

    private const val AUTH_TYPE_BASIC = "Basic"
  }

  @Service
  private class GetJBATokenSuccessRecorder {
    @Volatile
    private var currentState: Boolean? = null

    /**
     * Returns `true` if state changed, `false` otherwise
     */
    fun updateState(newState: Boolean): Boolean {
      val oldState = currentState
      currentState = newState
      return oldState != newState
    }

    companion object {
      fun getInstance(): GetJBATokenSuccessRecorder = service()
    }
  }

}