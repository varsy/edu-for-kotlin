package com.jetbrains.edu.ai.clippy.assistant.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.bindIntValue
import com.intellij.ui.dsl.builder.panel
import com.jetbrains.edu.ai.clippy.assistant.messages.EduAIClippyAssistantBundle
import com.jetbrains.edu.ai.settings.AIOptionsProvider

class EduAIClippyOptions : BoundConfigurable(EduAIClippyAssistantBundle.message("settings.ai.clippy.assistant")), AIOptionsProvider {
  private val settings = AIClippySettings.getInstance()
  private var tone: Int = settings.tone
  private var encouragementFrequency: Int = settings.encouragementFrequency
  private var emotionalIntensity: Int = settings.emotionalIntensity
  private var mistakesAttention: Int = settings.mistakesAttention
  private var communicationStyle: Int = settings.communicationStyle

  override fun createPanel(): DialogPanel= panel {
    group(displayName) {
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.tone")) {
        spinner(1..10)
          .bindIntValue(::tone)
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.encouragement.frequency")) {
        spinner(1..10)
          .bindIntValue(::encouragementFrequency)
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.emotional.intensity")) {
        spinner(1..10)
          .bindIntValue(::emotionalIntensity)
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.mistakes.attention")) {
        spinner(1..10)
          .bindIntValue(::mistakesAttention)
      }
      row(EduAIClippyAssistantBundle.message("settings.ai.clippy.communication.style")) {
        spinner(1..10)
          .bindIntValue(::communicationStyle)
      }
    }
  }

  override fun apply() {
    super.apply()
    val aiClippyProperties = AIClippyProperties(tone, encouragementFrequency, emotionalIntensity, mistakesAttention, communicationStyle)
    AIClippySettings.getInstance().setClippySettings(aiClippyProperties)
  }
}