package com.jetbrains.edu.aiDebugging.core.breakpoint

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AIBreakPointsStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    project.service<AIBreakPointService>().initialize()
  }
}
