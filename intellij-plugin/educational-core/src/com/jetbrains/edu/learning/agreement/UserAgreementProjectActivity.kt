package com.jetbrains.edu.learning.agreement

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.impl.ProjectViewPane
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.agreement.UserAgreementSettings.Companion.userAgreementSettings
import com.jetbrains.edu.learning.isHeadlessEnvironment
import com.jetbrains.edu.learning.projectView.CourseViewPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserAgreementProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isEduProject()) {
      return changeProjectView(project)
    }
    if (!userAgreementSettings().isNotShown && !isHeadlessEnvironment) {
      UserAgreementManager.getInstance().showUserAgreement(project)
    }
  }
  
  private suspend fun changeProjectView(project: Project) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW) ?: return
    val projectView = ProjectView.getInstance(project)
    val currentViewId = projectView.currentViewId
    withContext(Dispatchers.EDT) {
      if (CourseViewPane.ID == currentViewId) {
        projectView.changeView(ProjectViewPane.ID)
      }
      toolWindow.show()
    }
  }
}