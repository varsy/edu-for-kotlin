package com.jetbrains.edu.ai.clippy.assistant.settings

import com.intellij.openapi.components.*

@State(name = "AIClippySettings", storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE, roamingType = RoamingType.LOCAL)])
class AIClippySettings : PersistentStateComponent<AIClippySettings.State> {
  private var clippySettings = AIClippyProperties()

  val tone: Int get() = clippySettings.tone
  val encouragementFrequency: Int get() = clippySettings.encouragementFrequency
  val emotionalIntensity: Int get() = clippySettings.emotionalIntensity
  val mistakesAttention: Int get() = clippySettings.mistakesAttention
  val communicationStyle: Int get() = clippySettings.communicationStyle

  fun setClippySettings(settings: AIClippyProperties) {
    clippySettings = settings
  }

  override fun getState(): State {
    val state = State()
    state.tone = clippySettings.tone
    state.encouragementFrequency = clippySettings.encouragementFrequency
    state.emotionalIntensity = clippySettings.emotionalIntensity
    state.mistakesAttention = clippySettings.mistakesAttention
    state.communicationStyle = clippySettings.communicationStyle
    return state
  }

  override fun loadState(state: State) {
    clippySettings = AIClippyProperties(
      tone = state.tone,
      encouragementFrequency = state.encouragementFrequency,
      emotionalIntensity = state.emotionalIntensity,
      mistakesAttention = state.mistakesAttention,
      communicationStyle = state.communicationStyle
    )
  }

  class State : BaseState() {
    var tone by property(5)
    var encouragementFrequency by property(5)
    var emotionalIntensity by property(5)
    var mistakesAttention by property(5)
    var communicationStyle by property(5)
  }

  companion object {
    fun getInstance(): AIClippySettings = service()
  }
}