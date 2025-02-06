package com.jetbrains.edu.ai.clippy.assistant.settings

data class AIClippyProperties(
  /**
   * Sets the tone of the assistant.
   * The scale ranges from 1 (soft, friendly, and gentle) to 10 (harsh, strict, and direct).
   * A lower value makes the assistant provide comforting feedback, while higher values
   * result in more straightforward and motivating communication.
   */
  val tone: Int,
  /**
   * Configures how frequently the assistant encourages the user.
   * The scale ranges from 1 (rare encouragement, only in key situations)
   * to 10 (frequent encouragement with constant support).
   * A lower value means the assistant speaks sparingly, while higher values
   * result in more active and continuous encouragement.
   */
  val encouragementFrequency: Int,
  /**
   * Controls the emotional intensity of the assistant's feedback.
   * The scale ranges from 1 (neutral and professional tone) to 10 (highly emotional
   * and enthusiastic responses).
   * Lower values focus on reserved, unemotional messages, whereas higher values
   * bring lively and reinforcing messages like "Well done!" or "Amazing!"
   */
  val emotionalIntensity: Int,
  /**
   * Defines the assistant's focus between highlighting mistakes or achievements.
   * The scale ranges from 1 (greater focus on mistakes with constructive feedback)
   * to 10 (greater focus on achievements, with errors receiving less attention).
   * A lower value encourages the user to improve by pointing out errors, and
   * a higher value celebrates successes while minimizing criticism.
   */
  val mistakesAttention: Int,
  /**
   * Configures the assistant's communication style.
   * The scale ranges from 1 (formal, professional language) to 10 (casual, friendly, and personal tone).
   * A lower value keeps the tone serious and professional, whereas a higher value results
   * in warm, approachable, and engaging language.
   */
  val communicationStyle: Int
) {
  constructor() : this(5, 5, 5, 5, 5)

  init {
    require(tone in 1..10) {
      "Invalid value for tone: $tone. Tone must be between 1 (soft) and 10 (harsh)."
    }
    require(encouragementFrequency in 1..10) {
      "Invalid value for encouragementFrequency: $encouragementFrequency. Frequency must be between 1 (rare) and 10 (frequent)."
    }
    require(emotionalIntensity in 1..10) {
      "Invalid value for emotionalIntensity: $emotionalIntensity. Intensity must be between 1 (neutral) and 10 (highly emotional)."
    }
    require(mistakesAttention in 1..10) {
      "Invalid value for mistakesAttention: $mistakesAttention. Focus must be between 1 (mistakes) and 10 (achievements)."
    }
    require(communicationStyle in 1..10) {
      "Invalid value for communicationStyle: $communicationStyle. Style must be between 1 (formal) and 10 (friendly)."
    }
  }
}