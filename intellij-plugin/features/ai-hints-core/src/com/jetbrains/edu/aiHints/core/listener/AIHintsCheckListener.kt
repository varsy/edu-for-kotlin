package com.jetbrains.edu.aiHints.core.listener

import com.intellij.openapi.project.Project
import com.jetbrains.edu.aiHints.core.HintStateManager
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class AIHintsCheckListener : CheckListener {
  override fun beforeCheck(project: Project, task: Task) {
    HintStateManager.getInstance(project).reset()
  }
}