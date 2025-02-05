package com.jetbrains.edu.ai.clippy.assistant

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class ClippyService(private val project: Project) {
  @Volatile
  private var clippy: Clippy? = null
  private val lock = Mutex()

  suspend fun showClippy() {
    withContext(Dispatchers.EDT) {
      getClippy().show(project)
    }
  }

  private suspend fun getClippy(): Clippy =
    lock.withLock {
      if (clippy == null || clippy?.isDisposed == true) {
        clippy = Clippy()
      }
      clippy ?: error("Clippy is unexpectedly null")
    }

  companion object {
    fun getInstance(project: Project): ClippyService = project.service()

    fun isActive(project: Project): Boolean = getInstance(project).clippy?.isVisible == true
  }
}