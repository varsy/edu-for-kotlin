package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.InlineBanner
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.util.maximumHeight
import com.intellij.util.asSafely
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.isHeadlessEnvironment
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService
import com.jetbrains.edu.learning.submissions.SubmissionsListener
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.ui.MarketplaceSubmissionsTab
import com.jetbrains.edu.learning.submissions.ui.SubmissionsTab
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap.NavigationMapAction
import com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap.NavigationMapToolbar
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.theoryLookup.TheoryLookupTermsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

class TaskToolWindowViewImpl(project: Project, scope: CoroutineScope) : TaskToolWindowView(project), DataProvider, Disposable {
  private val lessonHeader: LessonHeader = LessonHeader()
  private val navigationMapToolbar: NavigationMapToolbar = NavigationMapToolbar()
  private val taskName: JLabel = JLabel(EduCoreBundle.message("item.task.title"))
  private val tabManager: TabManager = TabManager(project)
  private val checkPanel: CheckPanel = CheckPanel(project, this)

  init {
    Disposer.register(this, tabManager)

    scope.launch {
      TranslationProjectSettings.getInstance(project).translationProperties.collectLatest {
        withContext(Dispatchers.EDT) {
          updateHeaders()
          updateTaskDescription()
          ProjectView.getInstance(project).refresh()
        }
      }
      TheoryLookupTermsManager.getInstance(project).theoryLookupProperties.collectLatest {
        withContext(Dispatchers.EDT) {
          updateTaskDescription()
        }
      }
    }
  }

  override var currentTask: Task? = null
    // TODO: move it in some separate method
    set(value) {
      if (currentTask !== null && currentTask === value) return
      tabManager.updateTaskDescription(value)
      checkPanel.isVisible = value != null
      updateCheckPanel(value)
      updateNavigationPanel(value)
      updateHeaders(value)
      updateTabs(value)
      HyperskillMetricsService.getInstance().viewEvent(value)
      EduCounterUsageCollector.viewEvent(value)
      field = value
    }

  override fun updateTabs(task: Task?) {
    val taskToUpdate = task ?: currentTask
    tabManager.updateTabs(taskToUpdate)
  }

  override fun updateTab(tabType: TabType) {
    if (isHeadlessEnvironment) return
    val task = currentTask ?: return
    tabManager.updateTab(tabType, task)
  }

  override fun selectTab(tabType: TabType) {
    tabManager.selectTab(tabType)
  }

  override fun isSelectedTab(tabType: TabType): Boolean = tabManager.isSelectedTab(tabType)

  override fun showLoadingSubmissionsPanel(platformName: String) {
    if (currentTask == null) return
    val submissionsTab = getSubmissionTab() ?: return
    ApplicationManager.getApplication().invokeLater {
      submissionsTab.showLoadingPanel(platformName)
    }
  }

  override fun showLoadingCommunityPanel(platformName: String) {
    if (currentTask == null || !project.isMarketplaceCourse()) return

    val submissionsTab = getSubmissionTab() ?: return
    ApplicationManager.getApplication().invokeLater {
      submissionsTab.asSafely<MarketplaceSubmissionsTab>()?.showLoadingCommunityPanel(platformName)
    }
  }

  override fun showMyTab() {
    if (!project.isMarketplaceCourse()) return

    val submissionsTab = getSubmissionTab() ?: return
    project.invokeLater {
      submissionsTab.asSafely<MarketplaceSubmissionsTab>()?.showMyTab()
    }
  }

  override fun showCommunityTab() {
    if (!project.isMarketplaceCourse()) return

    val submissionsTab = getSubmissionTab() ?: return
    project.invokeLater {
      submissionsTab.asSafely<MarketplaceSubmissionsTab>()?.showCommunityTab()
    }
  }

  override fun isCommunityTabShowing(): Boolean {
    if (!project.isMarketplaceCourse()) return false
    val submissionsTab = getSubmissionTab() ?: return false
    return submissionsTab.asSafely<MarketplaceSubmissionsTab>()?.isCommunityTabShowing() ?: false
  }

  private fun getSubmissionTab(): SubmissionsTab? = tabManager.getTab(SUBMISSIONS_TAB) as? SubmissionsTab

  override fun updateCheckPanel(task: Task?) {
    if (task == null) return
    readyToCheck()
    checkPanel.updateCheckPanel(task)
  }

  override fun updateTaskSpecificPanel() {
    tabManager.updateTaskSpecificPanel(currentTask)
  }

  override fun updateNavigationPanel(task: Task?) {
    task ?: return

    lessonHeader.setHeaderText(task.lesson.presentableName)
    val course = StudyTaskManager.getInstance(project).course
    var index = 1
    val actions = task.lesson.taskList.map {
      val currentIndex = if (it is TheoryTask && course is HyperskillCourse) index else index++
      NavigationMapAction(it, task, currentIndex)
    }
    navigationMapToolbar.replaceActions(actions)

    if (course is HyperskillCourse) {
      lessonHeader.updateTopPanelForProblems(project, course, task)
    }
    scrollNavMap(task)
  }

  override fun updateNavigationPanel() = updateNavigationPanel(currentTask)

  override fun updateTaskDescriptionTab(task: Task?) {
    tabManager.updateTaskDescription(task)
  }

  private fun updateHeaders(task: Task? = currentTask) {
    if (task == null) {
      taskName.text = EduCoreBundle.message("item.task.title")
      return
    }

    val translationSettings = TranslationProjectSettings.getInstance(project)

    val translatedTaskName = translationSettings.getStudyItemTranslatedName(task)
    taskName.text = translatedTaskName ?: task.presentableName

    val lesson = task.lesson
    val translatedLessonName = translationSettings.getStudyItemTranslatedName(lesson)
    lessonHeader.setHeaderText(translatedLessonName ?: lesson.presentableName)
  }

  override fun updateTaskDescription() {
    updateTaskDescriptionTab(currentTask)
    updateCheckPanel(currentTask)
    scrollNavMap(currentTask)
  }

  override fun scrollNavMap(task: Task?) {
    task ?: return
    val selectedTask = navigationMapToolbar.components
                       ?.filterIsInstance<ActionButton>()
                       ?.find { (it.action as? NavigationMapAction)?.task == task } ?: return
    navigationMapToolbar.scrollRectToVisible(selectedTask.bounds)
   }

  override fun readyToCheck() {
    checkPanel.readyToCheck()
  }

  override fun init(toolWindow: ToolWindow) {
    //create main panel
    val mainPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
      border = JBUI.Borders.empty(0, 16, 12, 16)
    }
    Disposer.register(toolWindow.contentManager, tabManager)

    mainPanel.add(lessonHeader)

    // setup navigationMapToolbar
    navigationMapToolbar.targetComponent = mainPanel

    val navMapPanel = JBScrollPane(
      navigationMapToolbar,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
    ).apply {
          border = JBEmptyBorder(0)
          preferredSize = Dimension(0, 51)
          minimumSize = Dimension(0, 51)
          maximumHeight = 51
        }

    mainPanel.add(navMapPanel)

    // setup task name
    val taskNameBox = Box.createHorizontalBox()
    taskName.font = JBFont.h1()
    taskName.alignmentX = Component.LEFT_ALIGNMENT
    taskNameBox.add(taskName)
    taskNameBox.add(Box.createHorizontalGlue())
    taskNameBox.border = JBEmptyBorder(0,0,4,0)

    mainPanel.add(taskNameBox)

    mainPanel.add(tabManager.tabbedPane)

    mainPanel.add(checkPanel)

    UIUtil.setBackgroundRecursively(mainPanel, getTaskDescriptionBackgroundColor())

    val content = ContentFactory.getInstance()
      .createContent(mainPanel, EduCoreBundle.message("item.task.title"), false)
      .apply { isCloseable = false }
    toolWindow.contentManager.addContent(content)

    currentTask = project.getCurrentTask()
    updateTabs(currentTask)

    val connection = project.messageBus.connect()
    connection.subscribe(LafManagerListener.TOPIC, LafManagerListener {
      UIUtil.setBackgroundRecursively(mainPanel, getTaskDescriptionBackgroundColor())
    })
    connection.subscribe(EditorColorsManager.TOPIC, EditorColorsListener {
      updateAllTabs(project)
    })
    connection.subscribe(SubmissionsManager.TOPIC, SubmissionsListener {
      invokeLater {
        updateTab(SUBMISSIONS_TAB)
      }
    })
  }

  override fun checkStarted(task: Task, startSpinner: Boolean) {
    if (task != currentTask) return
    checkPanel.checkStarted(startSpinner)
  }

  override fun checkFinished(task: Task, checkResult: CheckResult) {
    if (task != currentTask) return
    checkPanel.updateCheckDetails(task, checkResult)
    if (task is DataTask || task.isChangedOnFailed) {
      updateCheckPanel(task)
    }
    if (checkResult.status == CheckStatus.Failed) {
      tabManager.updateTaskSpecificPanel(task)
    }
  }

  override fun getData(dataId: String): Any? {
    return if (PlatformDataKeys.HELP_ID.`is`(dataId)) {
      HELP_ID
    }
    else null
  }

  override fun addInlineBanner(inlineBanner: InlineBanner) {
    tabManager.descriptionTab.addInlineBanner(inlineBanner)
  }

  override fun addInlineBannerToCheckPanel(inlineBanner: InlineBanner) {
    checkPanel.addHint(inlineBanner)
    checkPanel.revalidate()
  }

  override fun dispose() {}

  companion object {
    private const val HELP_ID = "task.description"
  }

}
