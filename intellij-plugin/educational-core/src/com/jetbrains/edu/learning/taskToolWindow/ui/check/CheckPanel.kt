package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.InlineBannerBase
import com.intellij.util.Alarm
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.actions.*
import com.jetbrains.edu.learning.actions.EduAIHintsUtils.GET_HINT_ACTION_ID
import com.jetbrains.edu.learning.actions.EduAIHintsUtils.isGetHintAvailable
import com.jetbrains.edu.learning.checker.CheckUtils.getCustomRunConfigurationForRunner
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.stepik.hyperskill.actions.DownloadDatasetAction
import com.jetbrains.edu.learning.stepik.hyperskill.actions.RetryDataTaskAction
import com.jetbrains.edu.learning.taskToolWindow.addActionLinks
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.ui.getUICheckLabel
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JPanel

class CheckPanel(private val project: Project, private val parentDisposable: Disposable) : JPanel(BorderLayout()) {
  private val checkFinishedPanel: JPanel = JPanel(BorderLayout())
  private val checkActionsPanel: JPanel = JPanel(BorderLayout())
  private val linkPanel = JPanel(BorderLayout())
  private val checkDetailsPlaceholder: JPanel = JPanel(BorderLayout())
  private val leftActionsToolbar: JPanel = JPanel()
  private val checkButtonWrapper = JPanel(BorderLayout())
  private val getHintButtonWrapper = JPanel(BorderLayout())
  private val rightActionsToolbar: ActionToolbar = createRightActionToolbar()
  private val runActionsToolbar: ActionToolbar = createRunActionToolbar()
  private val course = project.course
  private val checkTimeAlarm: Alarm = Alarm(parentDisposable)
  private val asyncProcessIcon = AsyncProcessIcon("Submitting...")

  init {
    leftActionsToolbar.layout = BoxLayout(leftActionsToolbar, BoxLayout.X_AXIS)
    leftActionsToolbar.add(checkButtonWrapper)
    leftActionsToolbar.add(getHintButtonWrapper)
    leftActionsToolbar.add(runActionsToolbar.component)
    checkActionsPanel.add(leftActionsToolbar, BorderLayout.WEST)
    checkActionsPanel.add(checkFinishedPanel, BorderLayout.CENTER)
    checkActionsPanel.add(rightActionsToolbar.component, BorderLayout.EAST)
    checkActionsPanel.add(linkPanel, BorderLayout.NORTH)
    add(checkActionsPanel, BorderLayout.CENTER)
    add(checkDetailsPlaceholder, BorderLayout.NORTH)
    asyncProcessIcon.border = JBUI.Borders.empty(8, 6, 0, 10)
    maximumSize = Dimension(Int.MAX_VALUE, 30)
    border = JBUI.Borders.empty(2, 0, 0, 10)
  }

  fun readyToCheck() {
    addActionLinks(course, linkPanel, 10, 3)
    checkFinishedPanel.removeAll()
    checkDetailsPlaceholder.removeAll()
    checkTimeAlarm.cancelAllRequests()
  }

  fun checkStarted(startSpinner: Boolean) {
    readyToCheck()
    getHintButtonWrapper.removeAll()
    updateBackground()
    if (startSpinner) {
      checkFinishedPanel.add(asyncProcessIcon, BorderLayout.WEST)
    }
  }

  fun updateCheckDetails(task: Task, result: CheckResult? = null) {
    updateGetHintButtonWrapper(task)
    checkFinishedPanel.removeAll()
    checkFinishedPanel.addNextTaskButton(task)
    checkFinishedPanel.addRetryButton(task)

    val checkResult = result ?: restoreSavedResult(task)
    if (checkResult != null) {
      linkPanel.removeAll()
      checkDetailsPlaceholder.add(CheckDetailsPanel(project, task, checkResult, checkTimeAlarm), BorderLayout.SOUTH)
    }
    updateBackground()
  }

  private fun restoreSavedResult(task: Task): CheckResult? {
    val feedback = task.feedback
    if (feedback == null) {
      if (task.status == CheckStatus.Unchecked) {
        return null
      }
      return CheckResult(task.status)
    }
    else {
      if (task.isChangedOnFailed && task.status == CheckStatus.Failed) {
        feedback.message = EduCoreBundle.message("action.retry.shuffle.message")
      }
      return feedback.toCheckResult(task.status)
    }
  }

  private fun updateBackground() {
    UIUtil.setBackgroundRecursively(checkFinishedPanel, TaskToolWindowView.getTaskDescriptionBackgroundColor())
    UIUtil.setBackgroundRecursively(checkDetailsPlaceholder, TaskToolWindowView.getTaskDescriptionBackgroundColor())
    UIUtil.setBackgroundRecursively(checkButtonWrapper, TaskToolWindowView.getTaskDescriptionBackgroundColor())
  }

  fun updateCheckPanel(task: Task) {
    updateCheckButtonWrapper(task)
    updateCheckDetails(task)
  }

  fun addHint(inlineBanner: InlineBannerBase) {
    checkDetailsPlaceholder.add(inlineBanner, BorderLayout.NORTH)
  }

  private fun createRightActionToolbar(): ActionToolbar {
    val actionGroup = ActionManager.getInstance().getAction("Educational.CheckPanel.Right") as ActionGroup
    return ActionToolbarImpl(ACTION_PLACE, actionGroup, true).apply {
      targetComponent = this@CheckPanel
      setActionButtonBorder(2, 0)
      minimumButtonSize = Dimension(28, 28)
      border = JBEmptyBorder(5, 0, 0, 0)
    }
  }

  private fun createRunActionToolbar(): ActionToolbar {
    val actionGroup = ActionManager.getInstance().getAction("Educational.CheckPanel.Running") as ActionGroup
    return ActionToolbarImpl(ACTION_PLACE, actionGroup, true).apply {
      targetComponent = this@CheckPanel
      minimumButtonSize = JBDimension(28, 28)
      border = JBEmptyBorder(8, 0, 0, 0)
    }
  }

  private fun updateCheckButtonWrapper(task: Task) {
    checkButtonWrapper.removeAll()
    when (task) {
      is DataTask -> updateCheckButtonWrapper(task)
      is TheoryTask -> {}
      else -> {
        val isDefault = !(task.isChangedOnFailed && task.status == CheckStatus.Failed || task.isSolved)
        val isEnabled = !(task.isChangedOnFailed && task.status == CheckStatus.Failed)
        val checkComponent = CheckPanelButtonComponent(CheckAction(task.getUICheckLabel()), isDefault = isDefault, isEnabled = isEnabled)
        checkButtonWrapper.add(checkComponent, BorderLayout.WEST)
      }
    }
  }

  private fun updateCheckButtonWrapper(task: DataTask) {
    when (task.status) {
      CheckStatus.Unchecked -> {
        val isRunning = task.isRunning()
        val component = if (task.isTimeLimited && isRunning) {
          val endDateTime = task.attempt?.endDateTime ?: error("EndDateTime is expected")
          val checkTimer = CheckTimer(endDateTime) { updateCheckPanel(task) }
          Disposer.register(parentDisposable, checkTimer)
          checkTimer
        }
        else {
          CheckPanelButtonComponent(EduActionUtils.getAction(DownloadDatasetAction.ACTION_ID) as DownloadDatasetAction)
        }
        checkButtonWrapper.add(component, BorderLayout.WEST)

        val checkComponent = CheckPanelButtonComponent(CheckAction(task.getUICheckLabel()), isEnabled = isRunning, isDefault = isRunning)
        checkButtonWrapper.add(checkComponent, BorderLayout.CENTER)
      }
      CheckStatus.Failed, CheckStatus.Solved  -> {
        val retryComponent = CheckPanelButtonComponent(EduActionUtils.getAction(RetryDataTaskAction.ACTION_ID) as RetryDataTaskAction,
                                                       isDefault = true)
        checkButtonWrapper.add(retryComponent, BorderLayout.WEST)
      }
    }
  }

  private fun updateGetHintButtonWrapper(task: Task) {
    getHintButtonWrapper.removeAll()

    if (isGetHintAvailable(task) && EduAIHintsUtils.HintStateManager.isDefault(project)) {
      val action = ActionManager.getInstance().getAction(GET_HINT_ACTION_ID) as ActionWithProgressIcon
      getHintButtonWrapper.add(CheckPanelButtonComponent(action = action), BorderLayout.WEST)
    }
  }

  private fun JPanel.addNextTaskButton(task: Task) {
    if (!(task.status == CheckStatus.Solved
          || task is TheoryTask
          || task.course is HyperskillCourse
          || task.course.courseMode == CourseMode.EDUCATOR)) {
      return
    }

    val nextTask = NavigationUtils.nextTask(task)
    if (nextTask != null || (task.status == CheckStatus.Solved && NavigationUtils.isLastHyperskillProblem(task))) {
      updateCheckButtonWrapper(task) // to update the 'Check' button state
      val hasRunButton = getCustomRunConfigurationForRunner(project, task) != null
      val isDefault = (task is TheoryTask || task.isSolved) && !hasRunButton
      val action = ActionManager.getInstance().getAction(NextTaskAction.ACTION_ID)
      val nextButtonText = nextTask?.let { EduCoreBundle.message("button.next.task.text", nextTask.presentableName) }
      val nextButton = CheckPanelButtonComponent(action = action, isDefault = isDefault, customButtonText = nextButtonText)
      add(nextButton, BorderLayout.WEST)
    }
  }

  private fun JPanel.addRetryButton(task: Task) {
    if (!task.isChangedOnFailed) return

    if (task.status == CheckStatus.Failed) {
      val retryComponent = CheckPanelButtonComponent(EduActionUtils.getAction(RetryAction.ACTION_ID) as ActionWithProgressIcon,
        isDefault = true, isEnabled = true)
      add(retryComponent, BorderLayout.WEST)
    }
  }

  companion object {
    const val ACTION_PLACE = "CheckPanel"
  }
}
