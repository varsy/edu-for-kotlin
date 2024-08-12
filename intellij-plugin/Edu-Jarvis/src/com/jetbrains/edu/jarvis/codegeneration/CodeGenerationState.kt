package com.jetbrains.edu.jarvis.codegeneration

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
class CodeGenerationState {
  private val isBusy = AtomicBoolean(false)

  fun lock(): Boolean {
    return isBusy.compareAndSet(false, true)
  }

  fun unlock() {
    isBusy.set(false)
  }

  companion object {
    fun getInstance(project: Project): CodeGenerationState = project.service()
  }
}