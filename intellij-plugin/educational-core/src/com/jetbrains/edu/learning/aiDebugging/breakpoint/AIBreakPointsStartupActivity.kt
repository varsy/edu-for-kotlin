package com.jetbrains.edu.learning.aiDebugging.breakpoint

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class AIBreakPointsStartupActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    project.getService(AIBreakPointService::class.java).initialize()
  }
}
