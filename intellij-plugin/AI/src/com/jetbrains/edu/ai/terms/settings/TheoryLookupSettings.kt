package com.jetbrains.edu.ai.terms.settings

import com.intellij.openapi.components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@State(name = "TheoryLookupSettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.LOCAL)])
class TheoryLookupSettings : PersistentStateComponent<TheoryLookupSettings.State> {
  private val _theoryLookupProperties = MutableStateFlow<TheoryLookupProperties?>(null)
  val theoryLookupProperties = _theoryLookupProperties.asStateFlow()

  val isTheoryLookupEnabled: Boolean
    get() = _theoryLookupProperties.value?.isEnabled == true

  fun setTheoryLookupProperties(properties: TheoryLookupProperties) {
    _theoryLookupProperties.value = properties
  }

  override fun getState(): State {
    val state = State()
    state.isEnabled = isTheoryLookupEnabled
    return state
  }

  override fun loadState(state: State) {
    _theoryLookupProperties.value = TheoryLookupProperties(state.isEnabled)
  }

  class State : BaseState() {
    var isEnabled by property(false)
  }

  companion object {
    fun getInstance(): TheoryLookupSettings = service()
  }
}