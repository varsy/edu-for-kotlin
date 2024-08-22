package com.jetbrains.edu.learning.courseFormat.tasks.cognifire

/**
 * The PromptActions class represents a collection of prompt actions.
 * Each prompt action is associated with an element ID and a state (PromptWritten, CodeFailed, or CodeSuccess).
 * If all actions are generated successfully, the [generatedSuccessfully] returns true
 */
class PromptActions {

  private val actions: MutableSet<Prompt> = mutableSetOf()

  fun addAction(elementId: String) {
    if (actions.none { it.elementId == elementId }) {
      actions.add(Prompt(elementId, PromptCodeState.PromptWritten))
    }
  }

  fun updateAction(elementId: String, state: PromptCodeState) {
    actions.find { it.elementId == elementId }?.state = state
  }

  fun generatedSuccessfully() = actions.all {
    it.state == PromptCodeState.CodeSuccess
  }
}

data class Prompt(
  val elementId: String,
  var state: PromptCodeState
)
