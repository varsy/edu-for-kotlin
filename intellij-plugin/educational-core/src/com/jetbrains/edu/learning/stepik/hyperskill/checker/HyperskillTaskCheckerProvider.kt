package com.jetbrains.edu.learning.stepik.hyperskill.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle

class HyperskillTaskCheckerProvider(private val baseProvider: TaskCheckerProvider) : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = baseProvider.codeExecutor

  override val envChecker: EnvironmentChecker
    get() = baseProvider.envChecker

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask>? {
    val checker = baseProvider.getEduTaskChecker(task, project) ?: return null

    return object : TaskChecker<EduTask>(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult {
        val checkResult = checker.check(indicator)
        val course = task.course as HyperskillCourse
        val resultingStatus = checkResult.status

        if (resultingStatus == CheckStatus.Solved) {
          if (course.isTaskInProject(task)) {
            val projectLesson = course.getProjectLesson() ?: error("Unable to get project lesson")
            val otherUnsolvedTasks = projectLesson.taskList.filter { it != task && it.status != CheckStatus.Solved }
            if (otherUnsolvedTasks.isEmpty()) {
              return CheckResult(resultingStatus,
                                 EduCoreBundle.message("hyperskill.next.project", HYPERSKILL_PROJECTS_URL))
            }
          }

          return CheckResult(resultingStatus, CheckUtils.CONGRATULATIONS)
        }
        return checkResult
      }

      override fun onTaskSolved() = checker.onTaskSolved()
      override fun onTaskFailed() = checker.onTaskFailed()
      override fun clearState() = checker.clearState()
    }
  }
}