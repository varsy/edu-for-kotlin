package com.jetbrains.edu.learning.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.impl.NotificationSettings
import com.intellij.notification.impl.NotificationsConfigurationImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtilsKt.showPopup
import com.jetbrains.edu.learning.actions.EduActionUtils.showFakeProgress
import com.jetbrains.edu.learning.actions.EduActionUtils.updateAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckUtils.createTests
import com.jetbrains.edu.learning.checker.CheckUtils.deleteTests
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.details.CheckDetailsView
import com.jetbrains.edu.learning.checker.remote.RemoteTaskCheckerManager.remoteCheckerForTask
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.failedToCheck
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.projectView.ProgressUtil.updateCourseProgress
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.Companion.checkTask
import com.jetbrains.edu.learning.stepik.hyperskill.checker.HyperskillCheckConnector.failedToSubmit
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import com.jetbrains.edu.learning.ui.getUICheckLabel
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem
import org.jetbrains.annotations.NonNls
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CheckAction() : ActionWithProgressIcon(), DumbAware {

  constructor(@NlsActions.ActionText checkLabel: String) : this() {
    templatePresentation.text = checkLabel
  }

  init {
    setUpSpinnerPanel(PROCESS_MESSAGE)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (DumbService.isDumb(project)) {
      e.dataContext.showPopup(ActionUtil.getUnavailableMessage(message("check.title"), false))
      return
    }
    CheckDetailsView.getInstance(project).clear()
    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    if (!CheckActionState.getInstance(project).doLock()) {
      e.dataContext.showPopup(message("action.check.already.running"))
      return
    }
    for (listener in CheckListener.EP_NAME.extensionList) {
      listener.beforeCheck(project, task)
    }
    val checkTask = StudyCheckTask(project, task)
    if (isUnitTestMode) {
      // It's hack to make checker tests work properly.
      // `com.intellij.openapi.progress.ProgressManager.run(com.intellij.openapi.progress.Task)` executes task synchronously
      // if the task run in headless environment (e.g. in unit tests).
      // It blocks EDT and any next `ApplicationManager.getApplication().invokeAndWait()` call will hang because of deadlock
      val future = ApplicationManager.getApplication().executeOnPooledThread { ProgressManager.getInstance().run(checkTask) }
      EduActionUtils.waitAndDispatchInvocationEvents(future)
    }
    else {
      ProgressManager.getInstance().run(checkTask)
    }
  }

  override fun update(e: AnActionEvent) {
    if (CheckPanel.ACTION_PLACE == e.place) {
      //action is being added only in valid context, no project in event in this case, so just enable it
      return
    }
    updateAction(e)
    val project = e.project ?: return
    val taskFile = project.selectedTaskFile
    if (taskFile != null) {
      templatePresentation.text = taskFile.task.getUICheckLabel()
    }
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = !CheckActionState.getInstance(project).isLocked
      return
    }
    if (!isCourseCreator(project)) {
      return
    }
    val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(e.dataContext)
    if (virtualFile == null || FileEditorManager.getInstance(project).selectedTextEditor == null) {
      return
    }
    if (virtualFile.isTestsFile(project)) {
      e.presentation.isEnabledAndVisible = true
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private inner class StudyCheckTask(project: Project, private val task: Task)
    : com.intellij.openapi.progress.Task.Backgroundable(project, message("progress.title.checking.solution"), true) {
    private var result: CheckResult? = null
    private val checker: TaskChecker<*>?

    init {
      val configurator = task.course.configurator
      checker = configurator?.taskCheckerProvider?.getTaskChecker(task, project)
    }

    @RequiresEdt
    private fun onStarted(indicator: ProgressIndicator) {
      processStarted()
      ApplicationManager.getApplication().executeOnPooledThread { showFakeProgress(indicator) }
      TaskToolWindowView.getInstance(project).checkStarted(task, false)
    }

    override fun run(indicator: ProgressIndicator) {
      invokeAndWaitIfNeeded {
        onStarted(indicator)
      }
      val start = System.currentTimeMillis()
      val notificationSettings = turnOffTestRunnerNotifications()
      val localCheckResult = localCheck(indicator)
      invokeLater { NotificationsConfigurationImpl.getInstanceImpl().changeSettings(notificationSettings) }
      val end = System.currentTimeMillis()
      LOG.info(String.format("Checking of %s task took %d ms", task.name, end - start))
      if (localCheckResult.status === CheckStatus.Failed) {
        result = localCheckResult
        return
      }
      val remoteChecker = remoteCheckerForTask(project, task)
      result = remoteChecker?.check(project, task, indicator) ?: localCheckResult
    }

    private fun localCheck(indicator: ProgressIndicator): CheckResult {
      if (checker == null) return CheckResult.NO_LOCAL_CHECK
      task.getDir(project.courseDir) ?: return CheckResult.NO_LOCAL_CHECK

      if (task.course.isStudy) {
        createTests(invisibleTestFiles, project)
      }
      return try {
        checker.check(indicator)
      }
      finally {
        if (task.shouldGenerateTestsOnTheFly()) {
          deleteTests(invisibleTestFiles, project)
        }
      }
    }

    private val invisibleTestFiles: List<TaskFile>
      get() = task.taskFiles.values.filter {
        EduUtilsKt.isTestsFile(task, it.name) && !it.isVisible && (task is EduTask || task is OutputTask)
      }

    override fun onSuccess() {
      val checkResult = result
      requireNotNull(checkResult)

      if (task.course.isStudy) {
        task.status = checkResult.status
        task.feedback = CheckFeedback(Date(), checkResult)
        saveItem(task)
      }
      if (checker != null) {
        if (checkResult.status === CheckStatus.Failed) {
          checker.onTaskFailed()
        }
        else if (checkResult.status === CheckStatus.Solved) {
          checker.onTaskSolved()
        }
      }
      checkTask(task.status)
      TaskToolWindowView.getInstance(project).checkFinished(task, checkResult)
      project.invokeLater {
        updateCourseProgress(project)
        ProjectView.getInstance(project).refresh()
        for (listener in CheckListener.EP_NAME.extensions) {
          listener.afterCheck(project, task, checkResult)
        }
      }
    }

    override fun onCancel() {
      TaskToolWindowView.getInstance(project).readyToCheck()
    }

    override fun onFinished() {
      checker?.clearState()
      processFinished()
      CheckActionState.getInstance(project).unlock()
    }

    override fun onThrowable(error: Throwable) {
      super.onThrowable(error)
      if (error.message == message("error.failed.to.refresh.tokens")) {
        TaskToolWindowView.getInstance(project).checkFinished(task, failedToSubmit(project, task, message("error.failed.to.refresh.tokens")))
      }
      else {
        TaskToolWindowView.getInstance(project).checkFinished(task, failedToCheck)
      }
    }

    private fun turnOffTestRunnerNotifications(): NotificationSettings {
      val notificationsConfiguration = NotificationsConfigurationImpl.getInstanceImpl()
      val testRunnerSettings = NotificationsConfigurationImpl.getSettings(TEST_RESULTS_DISPLAY_ID)
      notificationsConfiguration.changeSettings(TEST_RESULTS_DISPLAY_ID, NotificationDisplayType.NONE, false, false)
      return testRunnerSettings
    }

    private val TEST_RESULTS_DISPLAY_ID: @NonNls String = "Test Results: Run"
  }

  @Service(Service.Level.PROJECT)
  private class CheckActionState {
    private val isBusy = AtomicBoolean(false)
    fun doLock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    val isLocked: Boolean
      get() = isBusy.get()

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): CheckActionState {
        return project.service()
      }
    }
  }

  companion object {
    private const val PROCESS_MESSAGE = "Check in progress"
    private val LOG = Logger.getInstance(CheckAction::class.java)
  }
}
