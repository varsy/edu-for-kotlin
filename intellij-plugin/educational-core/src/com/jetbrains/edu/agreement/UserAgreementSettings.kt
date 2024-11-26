package com.jetbrains.edu.agreement

import com.intellij.openapi.components.*
import com.intellij.util.application
import com.jetbrains.edu.learning.submissions.UserAgreementState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Service
@State(name = "UserAgreementSettings", storages = [Storage(StoragePathMacros.APP_INTERNAL_STATE_DB, roamingType = RoamingType.DEFAULT)])
class UserAgreementSettings : PersistentStateComponent<UserAgreementSettings.State> {
  private val _userAgreementProperties = MutableStateFlow(UserAgreementProperties())
  val userAgreementProperties = _userAgreementProperties.asStateFlow()

  val isPluginAllowed: Boolean
    get() {
      val pluginAgreement = userAgreementProperties.value.pluginAgreement
      return pluginAgreement == UserAgreementState.ACCEPTED || pluginAgreement == UserAgreementState.NOT_SHOWN
    }

  val pluginAgreement: Boolean
    get() = _userAgreementProperties.value.pluginAgreement == UserAgreementState.ACCEPTED

  val isNotShown: Boolean
    get() = userAgreementProperties.value.pluginAgreement == UserAgreementState.NOT_SHOWN

  fun setUserAgreementSettings(settings: UserAgreementProperties) {
    _userAgreementProperties.value = settings
  }

  companion object {
    fun userAgreementSettings(): UserAgreementSettings = application.service()
  }

  data class UserAgreementProperties(
    val pluginAgreement: UserAgreementState = UserAgreementState.NOT_SHOWN
  )

  class State : BaseState() {
    var pluginAgreement by enum<UserAgreementState>(UserAgreementState.NOT_SHOWN)
  }

  override fun getState(): State {
    val state = State()
    state.pluginAgreement = userAgreementProperties.value.pluginAgreement
    return state
  }

  override fun loadState(state: State) {
    _userAgreementProperties.value = UserAgreementProperties(state.pluginAgreement)
  }
}