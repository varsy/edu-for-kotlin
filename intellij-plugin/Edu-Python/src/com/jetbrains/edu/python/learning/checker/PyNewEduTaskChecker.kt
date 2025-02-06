package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.checker.EduTaskCheckerBase
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.tests.TestResultCollector
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.python.testing.AbstractPythonTestRunConfiguration
import com.jetbrains.python.testing.PythonTestConfigurationType
import com.jetbrains.python.testing.TestRunnerService

class PyNewEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val module = ModuleManager.getInstance(project).modules.singleOrNull()
    TestRunnerService.getInstance(module).selectedFactory = PythonTestConfigurationType.getInstance().unitTestFactory
    // In general, python plugin can create run configuration for a directory,
    // but it can skip some test files if they haven't proper names
    return createTestConfigurationsForTestFiles()
  }

  override fun createTestConfigurationFromPsiElement(element: PsiElement): RunnerAndConfigurationSettings? {
    val configuration = super.createTestConfigurationFromPsiElement(element)
    configuration?.setTaskDirAsWorking()
    return configuration
  }

  override fun ConfigurationContext.selectPreferredConfiguration(): RunnerAndConfigurationSettings? {
    return configuration?.takeIf { it.configuration is AbstractPythonTestRunConfiguration<*> }
  }

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult =
    PyStderrAnalyzer().tryToGetCheckResult(stderr) ?: CheckResult.SOLVED

  override fun createTestResultCollector(): TestResultCollector = PyTestResultCollector()

  private fun RunnerAndConfigurationSettings.setTaskDirAsWorking() {
    val pythonConfiguration = configuration as? AbstractPythonTestRunConfiguration<*>
    pythonConfiguration?.workingDirectory = task.getDir(project.courseDir)?.path
  }
}
