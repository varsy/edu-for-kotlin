package com.jetbrains.edu.aiHints.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project


@Service(Service.Level.PROJECT)
class HintStateManager {
  @Volatile
  private var state: HintState = HintState.DEFAULT

  private enum class HintState {
    DEFAULT, ACCEPTED;
  }

  fun reset() {
    state = HintState.DEFAULT
  }

  fun acceptHint() {
    state = HintState.ACCEPTED
  }

  companion object {
    fun isDefault(project: Project): Boolean = getInstance(project).state == HintState.DEFAULT

    fun getInstance(project: Project): HintStateManager = project.service()
  }
}