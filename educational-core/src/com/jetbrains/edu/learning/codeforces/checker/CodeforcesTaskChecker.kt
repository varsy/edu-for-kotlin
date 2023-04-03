package com.jetbrains.edu.learning.codeforces.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.OutputTaskCheckerBase
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CodeforcesTaskChecker(
  task: CodeforcesTask,
  envChecker: EnvironmentChecker,
  project: Project,
  codeExecutor: CodeExecutor
) : OutputTaskCheckerBase<CodeforcesTask>(task, envChecker, project, codeExecutor) {
  override fun getTestFolders(project: Project, task: CodeforcesTask): Array<out VirtualFile> {
    return task.getTestFolders(project)
  }
  override fun getIncorrectMessage(testFolderName: String): String = EduCoreBundle.message("codeforces.test.failed", testFolderName)
  override fun processCorrectCheckResult(): CheckResult {
    /**
     * Message will be added in
     * @see com.jetbrains.edu.learning.taskDescription.ui.check.CheckDetailsPanel.createCodeforcesSuccessMessagePanel
     * It's better not to reuse hyperlinkListener mechanism.
     *
     * We need to check the status of Codeforces task in checker tests, e.g.
     * @see com.jetbrains.edu.python.slow.checker.PyCodeforcesCheckerTest and similar
     */
    return if (isUnitTestMode) CheckResult.SOLVED else CheckResult.UNCHECKED
  }

  override fun compareOutputs(expected: String, actual: String): Boolean {
    return expected.trimEnd('\n') != actual.trimEnd('\n')
  }

  override fun createLatestOutputFile(testFolder: VirtualFile, actualOutput: String) {
    GeneratorUtils.createTextChildFile(project, testFolder, task.latestOutputFileName, actualOutput)
  }
}
