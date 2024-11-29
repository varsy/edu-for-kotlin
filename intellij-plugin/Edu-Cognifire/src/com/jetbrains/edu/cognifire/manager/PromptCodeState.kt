package com.jetbrains.edu.cognifire.manager

/**
 * Represents the state for each Prompt-Code block
 */
enum class PromptCodeState {

  /**
   * prompt is written but the user hasn't run it yet
   */
  PromptWritten,

  /**
   * the code is generated incorrectly - it does not follow the grammar rules or contains TODO blocks
   */
  CodeFailed,

  /**
   * the code is generated correctly
   */
  CodeSuccess
}
