package com.jetbrains.edu.learning.courseFormat.eduAssistant

/**
 * Represents the possible states of the AI edu assistant.
 */
enum class AiAssistantState {
  /**
   * A base empty state
   */
  NotInitialized,

  /**
   * This state is set when the context has been initialized by the user request directly
   */
  HelpAsked,
}
