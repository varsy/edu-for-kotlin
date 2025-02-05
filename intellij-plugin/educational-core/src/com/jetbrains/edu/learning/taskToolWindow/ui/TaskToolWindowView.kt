package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.InlineBannerBase
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isHeadlessEnvironment
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import org.jetbrains.annotations.TestOnly
import java.awt.Color

abstract class TaskToolWindowView(val project: Project) : EduTestAware {

  abstract var currentTask: Task?

  abstract fun init(toolWindow: ToolWindow)

  abstract fun selectTab(tabType: TabType)
  abstract fun isSelectedTab(tabType: TabType): Boolean
  abstract fun updateCheckPanel(task: Task?)
  abstract fun updateTaskSpecificPanel()
  abstract fun updateNavigationPanel(task: Task?)
  abstract fun updateNavigationPanel()
  abstract fun updateTaskDescriptionTab(task: Task?)
  abstract fun updateTaskDescription()
  abstract fun updateTabs(task: Task? = null)
  abstract fun updateTab(tabType: TabType)
  abstract fun showLoadingSubmissionsPanel(platformName: String)
  abstract fun showLoadingCommunityPanel(platformName: String)
  abstract fun showMyTab()
  abstract fun showCommunityTab()
  abstract fun isCommunityTabShowing(): Boolean
  abstract fun readyToCheck()
  abstract fun scrollNavMap(task: Task?)
  abstract fun checkStarted(task: Task, startSpinner: Boolean = false)
  abstract fun checkFinished(task: Task, checkResult: CheckResult)
  abstract fun addInlineBanner(inlineBanner: InlineBannerBase)
  abstract fun addInlineBannerToCheckPanel(inlineBanner: InlineBannerBase)

  @TestOnly
  override fun cleanUpState() {
    currentTask = null
  }

  companion object {

    fun getInstance(project: Project): TaskToolWindowView {
      if (!project.isEduProject()) {
        error("Attempt to get TaskDescriptionView for non-edu project")
      }
      return project.service()
    }

    fun getTaskDescriptionBackgroundColor(): Color {
      return UIUtil.getListBackground()
    }

    fun updateAllTabs(project: Project) {
      if (isHeadlessEnvironment) return
      getInstance(project).apply {
        updateTaskDescription()
        updateTabs()
      }
    }
  }
}
