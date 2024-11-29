package com.jetbrains.edu.cognifire.manager

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent

/**
 * The PromptActionManager class represents a collection of prompt actions.
 * Each prompt action is associated with an element ID, a promptToCode and a state (PromptWritten, CodeFailed, or CodeSuccess).
 * If all actions are generated successfully, the [generatedSuccessfully] returns true
 */
@Service(Service.Level.PROJECT)
class PromptActionManager {

  private val actions: MutableSet<PromptAction> = mutableSetOf()

  fun addAction(elementId: String, taskId: Int) {
    getAction(elementId)?.apply {
      this.taskId = taskId
    } ?: actions.add(PromptAction(elementId, taskId, PromptCodeState.PromptWritten, null))
  }

  fun updateAction(elementId: String, newState: PromptCodeState, promptToCodeContent: PromptToCodeContent) {
    getAction(elementId)?.apply {
      promptToCode = promptToCodeContent
      state = newState
    }
  }

  fun getAction(elementId: String) = actions.find { it.elementId == elementId }

  fun generatedSuccessfully(taskId: Int) = actions.filter { it.taskId == taskId }.none {
    it.state == PromptCodeState.CodeFailed
  }

  companion object {
    fun getInstance(project: Project): PromptActionManager = project.service()
  }
}

data class PromptAction(
  val elementId: String,
  var taskId: Int,
  var state: PromptCodeState,
  var promptToCode: PromptToCodeContent? = null
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
