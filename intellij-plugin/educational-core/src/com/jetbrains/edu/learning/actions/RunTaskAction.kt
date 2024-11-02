package com.jetbrains.edu.learning.actions

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckUtils.getCustomRunConfigurationForRunner
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.selectedTaskFile

class RunTaskAction : ActionWithProgressIcon() {

  init {
    setUpSpinnerPanel("Run in progress")
    templatePresentation.text = EduCoreBundle.message("action.Educational.Run.text")
  }

  override fun actionPerformed(e: AnActionEvent) {
    processStarted()

    ApplicationManager.getApplication().executeOnPooledThread {
      doRun(e.project)

      invokeLater { processFinished() }
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isVisible = true

    e.presentation.isEnabled = false
    val project = e.project ?: return
    val taskFile = project.selectedTaskFile ?: return
    val customRunConfigurationForRunner = getCustomRunConfigurationForRunner(project, taskFile.task) ?: return

    e.presentation.isEnabled = true
    logger<RunTaskAction>().info("RunTaskAction update: found custom run configuration ${customRunConfigurationForRunner.name}")
  }

  private fun doRun(project: Project?) {
    project ?: return

    val taskFile = project.selectedTaskFile ?: return
    val task = taskFile.task

    val runnerAndConfigurationSettings = getCustomRunConfigurationForRunner(project, task)
    if (runnerAndConfigurationSettings == null) {
      logger<RunTaskAction>().warn("Failed to find custom run configuration for runner in ${task.name}")
      return
    }

    try {
      runnerAndConfigurationSettings.checkSettings()
    }
    catch (e: RuntimeConfigurationException) {
      logger<RunTaskAction>().warn("Custom run configuration \"${runnerAndConfigurationSettings.name}\" has warnings in settings: ${e.messageHtml}")
    }
    catch (e: RuntimeConfigurationError) {
      logger<RunTaskAction>().warn("Custom run configuration \"${runnerAndConfigurationSettings.name}\" has error in settings: ${e.messageHtml}")
      return
    }

    val runner = ProgramRunner.getRunner(DefaultRunExecutor.EXECUTOR_ID, runnerAndConfigurationSettings.configuration)
    if (runner == null) {
      logger<RunTaskAction>().error("Failed to find runner for custom run configuration: ${runnerAndConfigurationSettings.name}")
      return
    }
    val env = ExecutionEnvironmentBuilder.create(
      DefaultRunExecutor.getRunExecutorInstance(),
      runnerAndConfigurationSettings
    ).activeTarget().build()

    runner.execute(env)
  }

  companion object {
    const val ACTION_ID = "Educational.Run"
    const val RUN_CONFIGURATION_FILE_NAME = "runner.run.xml"
  }
}