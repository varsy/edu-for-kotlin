package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.process.*
import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.checker.CheckUtils.fillWithIncorrect
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckResult.Companion.noTestsRun
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduTestInfo
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.Companion.firstFailed
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestDirectories
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestFiles
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
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

    val testRoots = mutableListOf<SMTestProxy.SMRootTestProxy>()
    val testEventsListener = object : SMTRunnerEventsAdapter() {
      // We have to collect test roots in `onTestingStarted`
      // because some test framework integrations (Gradle)
      // have custom components in implementation that don't provide test events
      // except `onTestingStarted`
      override fun onTestingStarted(testsRoot: SMTestProxy.SMRootTestProxy) {
        testRoots += testsRoot
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

    if (!CheckUtils.executeRunConfigurations(
        project,
        configurations,
        indicator,
        processListener = processListener,
        testEventsListener = testEventsListener
      )
    ) {
      LOG.warn("Execution failed because the configuration is broken")
      return noTestsRun
    }

    // We need to invoke all current pending EDT actions to get proper states of test roots.
    invokeAndWaitIfNeeded {}

    if (indicator.isCanceled) return CheckResult.CANCELED

    if (areTestsFailedToRun(testRoots)) {
      val result = computePossibleErrorResult(indicator, stderr.toString())
      if (!result.isSolved) {
        return result
      }
    }

    val testResults = testRoots.map { it.toCheckResult() }
    if (testResults.isEmpty()) return noTestsRun

    val firstFailure = testResults.firstOrNull { it.status != CheckStatus.Solved }
    return firstFailure ?: testResults.first()
  }

  protected fun SMTestProxy.SMRootTestProxy.toCheckResult(): CheckResult {
    val testInfo = getEduTestInfo(children = children)
    if (finishedSuccessfully()) {
      return CheckResult(status = CheckStatus.Solved, message = CheckUtils.CONGRATULATIONS, executedTestsInfo = testInfo)
    }

    if (testInfo.firstFailed() == null) {
      error("Testing failed although no failed tests found")
    }
    return CheckResult(status = CheckStatus.Failed, executedTestsInfo = testInfo)
  }

  private fun SMTestProxy.finishedSuccessfully(): Boolean {
    return !hasErrors() && (isPassed || isIgnored)
  }

  /**
   * Launches additional task if tests cannot be run (e.g. because of compilation error or syntax error).
   * Main purpose is to get proper error message for such cases.
   * If test framework support already provides correct message, you don't need to override this method
   */
  protected open fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult = CheckResult.SOLVED

  /**
   * Check if the launch of tests was not successful. It allows us to create meaningful output in such cases.
   */
  protected open fun areTestsFailedToRun(testRoots: List<SMTestProxy.SMRootTestProxy>): Boolean = testRoots.all { it.children.isEmpty() }

  protected fun createTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val customConfiguration = CheckUtils.getCustomRunConfiguration(project, task)
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

  /**
   * Returns message for test error that will be shown to a user in Check Result panel
   */
  @Suppress("UnstableApiUsage")
  @NlsSafe
  protected open fun getErrorMessage(node: SMTestProxy): String = node.errorMessage ?: EduCoreBundle.message("error.execution.failed")

  /**
   * Returns message for comparison error that will be shown to a user in Check Result panel
   */
  protected open fun getComparisonErrorMessage(node: SMTestProxy): String = getErrorMessage(node)

  protected open fun validateConfiguration(configuration: RunnerAndConfigurationSettings): CheckResult? = null

  private fun getEduTestInfo(paths: MutableList<String> = mutableListOf(), children: List<SMTestProxy>): List<EduTestInfo> {
    val result = mutableListOf<EduTestInfo>()
    children.forEach {
      paths.add(it.presentableName)
      if (it.isLeaf) {
        // Submission Service stores a test name with a maximum length of 255 characters.
        // The number 245 is chosen to allow space for [1], [2], etc., for possible duplicate test names.
        var testName = paths.joinToString(":").take(245)
        val testCount = result.count { test -> test.name == testName }
        if (testCount > 0) {
          testName = "$testName[$testCount]"
        }
        result.add(it.toEduTestInfo(testName))
      }
      else {
        result.addAll(getEduTestInfo(paths, it.children))
      }
      paths.removeAt(paths.size - 1)
    }
    return result
  }

  private fun SMTestProxy.toEduTestInfo(name: String): EduTestInfo {
    val diff = diffViewerProvider
    val message = if (diff != null) getComparisonErrorMessage(this) else getErrorMessage(this)
    return EduTestInfo(
      name = name,
      status = magnitudeInfo.value,
      message = removeAttributes(fillWithIncorrect(message)),
      details = stacktrace,
      isFinishedSuccessfully = finishedSuccessfully(),
      checkResultDiff = diff?.let { CheckResultDiff(diff.left, diff.right, diff.diffTitle) }
    )
  }

  companion object {
    fun extractComparisonErrorMessage(node: SMTestProxy): String {
      val errorMessage = node.errorMessage.orEmpty()
      val index = StringUtil.indexOfIgnoreCase(errorMessage, "expected:", 0)
      return if (index != -1) errorMessage.substring(0, index).trim() else errorMessage
    }

    /**
     * Some testing frameworks add attributes to be shown in console (ex. Jest - ANSI color codes)
     * which are not supported in Task Description, so they need to be removed
     */
    fun removeAttributes(text: String): String {
      val buffer = StringBuilder()
      AnsiEscapeDecoder().escapeText(text, ProcessOutputTypes.STDOUT) { chunk, _ ->
        buffer.append(chunk)
      }
      return buffer.toString()
    }

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
