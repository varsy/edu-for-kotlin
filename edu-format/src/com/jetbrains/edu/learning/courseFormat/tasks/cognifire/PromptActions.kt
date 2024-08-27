package com.jetbrains.edu.learning.courseFormat.tasks.cognifire

/**
 * The PromptActionManager class represents a collection of prompt actions.
 * Each prompt action is associated with an element ID and a state (PromptWritten, CodeFailed, or CodeSuccess).
 * If all actions are generated successfully, the [generatedSuccessfully] returns true
 */
class PromptActionManager {

  private val actions: MutableSet<PromptAction> = mutableSetOf()

  fun addAction(elementId: String) {
    actions.add(PromptAction(elementId, PromptCodeState.PromptWritten))
  }

  fun updateAction(elementId: String, state: PromptCodeState) {
    actions.find { it.elementId == elementId }?.state = state
  }

  fun generatedSuccessfully() = actions.all {
    it.state == PromptCodeState.CodeSuccess
  }
}

data class PromptAction(
  val elementId: String,
  var state: PromptCodeState
) {

  override fun hashCode(): Int = elementId.hashCode()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    return when (other as? PromptAction) {
      null -> false
      else -> elementId == other.elementId
    }
  }
}
