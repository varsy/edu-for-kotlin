package com.jetbrains.edu.ai.clippy.assistant.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.ai.clippy.assistant.Clippy
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class ShowClippyAssistant : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    Clippy.show(project)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = true
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  companion object {
    @Suppress("unused")
    @NonNls
    private const val ACTION_ID: String = "Educational.Student.ShowClippyAssistant"
  }
}