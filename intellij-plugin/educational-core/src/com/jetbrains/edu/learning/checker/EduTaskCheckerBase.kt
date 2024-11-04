package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.checker.tests.SMTestResultCollector
import com.jetbrains.edu.learning.checker.tests.TestResultCollector
import com.jetbrains.edu.learning.checker.tests.TestResultGroup
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.noTestsRun
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestFiles
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.runReadActionInSmartMode

abstract class EduTaskCheckerBase(task: EduTask, private val envChecker: EnvironmentChecker, project: Project) :
  TaskChecker<EduTask>(task, project) {

  var activateRunToolWindow: Boolean = !task.course.isStudy

  override fun check(indicator: ProgressIndicator): CheckResult {
    if (task.course.isStudy) {
      runInEdt {
        ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.RUN)?.hide(null)
      }
    }

    val possibleError = envChecker.getEnvironmentError(project, task)
    if (possibleError != null) {
      return possibleError
    }

    val configurations = runReadActionInSmartMode(project) { createTestConfigurations() }
    configurations.forEach {
      it.isActivateToolWindowBeforeRun = activateRunToolWindow
    }

    if (configurations.isEmpty()) return noTestsRun

    configurations.forEach {
      val validationResult = validateConfiguration(it)
      if (validationResult != null) {
        return validationResult
      }
    }

    val stderr = StringBuilder()
    val processListener = object : ProcessAdapter() {
      override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text
        if (text != null && ProcessOutputType.isStderr(outputType)) {
          stderr.append(text)
        }
      }
    }

    val testResultCollector = createTestResultCollector()
    if (!CheckUtils.executeRunConfigurations(
        project,
        configurations,
        indicator,
        processListener = processListener,
        testResultCollector = testResultCollector
      )
    ) {
      LOG.warn("Execution failed because the configuration is broken")
      return noTestsRun
    }

    // We need to invoke all current pending EDT actions to get proper states of test roots.
    invokeAndWaitIfNeeded {}

    if (indicator.isCanceled) return CheckResult.CANCELED

    val testResults = testResultCollector.collectTestResults()

    if (areTestsFailedToRun(testResults)) {
      val result = computePossibleErrorResult(indicator, stderr.toString())
      if (!result.isSolved) {
        return result
      }
    }

    if (testResults.isEmpty()) return noTestsRun
    val checkResults = testResults.map { CheckResult.from(it) }

    val firstFailure = checkResults.firstOrNull { it.status != CheckStatus.Solved }
    return firstFailure ?: checkResults.first()
  }

  private fun CheckResult.Companion.from(group: TestResultGroup): CheckResult {
    return if (group.results.firstFailed() != null) {
      CheckResult(status = CheckStatus.Failed, executedTestsInfo = group.results)
    }
    else {
      CheckResult(status = CheckStatus.Solved, message = CheckUtils.CONGRATULATIONS, executedTestsInfo = group.results)
    }
  }

  /**
   * Creates new instance of [TestResultCollector] to collect test results for the corresponding technology
   * during running test run configurations
   */
  protected open fun createTestResultCollector(): TestResultCollector = SMTestResultCollector()

  /**
   * Launches additional task if tests cannot be run (e.g. because of compilation error or syntax error).
   * Main purpose is to get proper error message for such cases.
   * If test framework support already provides correct message, you don't need to override this method
   */
  protected open fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult = CheckResult.SOLVED

  /**
   * Check if the launch of tests was not successful. It allows us to create meaningful output in such cases.
   */
  protected open fun areTestsFailedToRun(testResults: List<TestResultGroup>): Boolean = testResults.isEmpty()

  protected fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val customConfiguration = CheckUtils.getCustomRunConfigurationForChecker(project, task)
    return if (customConfiguration != null) {
      listOf(customConfiguration)
    }
    else {
      val defaultConfigurations = createDefaultTestConfigurations()
      // Only default run configurations should be marked as temporary
      // Otherwise, custom run configurations will be removed from the disk by the platform
      defaultConfigurations.forEach { it.isTemporary = true }
      defaultConfigurations
    }
  }

  /**
   * Creates and return list of run configurations to run task tests.
   *
   * In case, when support of particular language can create single run configuration from [com.intellij.psi.PsiDirectory] context,
   * it should return only one configuration per test directory. Otherwise, returns one run configuration per test file
   *
   * @return Run configurations to run task tests
   */
  protected abstract fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings>

  protected fun createTestConfigurationsForTestFiles(): List<RunnerAndConfigurationSettings> {
    return task.getAllTestFiles(project).mapNotNull { createTestConfigurationFromPsiElement(it) }
  }

  protected fun createTestConfigurationsForTestDirectories(): List<RunnerAndConfigurationSettings> {
    return task.getAllTestDirectories(project).mapNotNull { createTestConfigurationFromPsiElement(it) }
  }

  protected open fun createTestConfigurationFromPsiElement(element: PsiElement): RunnerAndConfigurationSettings? {
    return ConfigurationContext(element).selectPreferredConfiguration()
  }

  /**
   * Provides hint for checker what configuration should be preferred according to its type.
   * Should help when several run configurations are provided by platform and plugins
   *
   * @see com.jetbrains.edu.learning.checker.EduTaskCheckerBase.selectPreferredConfiguration
   */
  protected open val preferredConfigurationType: ConfigurationType? = null

  protected open fun ConfigurationContext.selectPreferredConfiguration(): RunnerAndConfigurationSettings? {
    val comparator = createConfigurationTypeComparator(preferredConfigurationType)
    return configurationsFromContext?.sortedWith(comparator)?.firstOrNull()?.configurationSettings
  }

  protected open fun validateConfiguration(configuration: RunnerAndConfigurationSettings): CheckResult? = null

  companion object {

    private fun createConfigurationTypeComparator(configurationType: ConfigurationType?): Comparator<ConfigurationFromContext> {
      return Comparator { c1, c2 ->
        when {
          c1.configurationType == configurationType && c2.configurationType != configurationType -> -1
          c1.configurationType != configurationType && c2.configurationType == configurationType -> 1
          else -> 0
        }
      }
    }
  }
}
