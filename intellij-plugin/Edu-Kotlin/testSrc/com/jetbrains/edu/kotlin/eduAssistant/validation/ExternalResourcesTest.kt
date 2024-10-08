package com.jetbrains.edu.kotlin.eduAssistant.validation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.jetbrains.edu.ai.hints.validation.util.downloadSolution
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerFixture
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.checker.CheckersTestCommonBase
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.educational.ml.hints.assistant.AiAssistantHint
import com.jetbrains.educational.ml.hints.assistant.AiHintsAssistant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
abstract class ExternalResourcesTest(private val lessonName: String, private val taskName: String) : CheckersTestCommonBase<JdkProjectSettings>() {

  protected abstract val course: Course

  override fun setUp() {
    super.setUp()

    course.lessons.flatMap { lesson ->
      lesson.taskList.filterIsInstance<EduTask>()
    }.map {
      val taskDir = it.getDir(project.courseDir) ?: error("Cannot find a task dir for task ${it.name}")
      File(taskDir.path).walk().forEach { file ->
        if (file.isFile) {
          VfsRootAccess.allowRootAccess(testRootDisposable, file.path)
        }
      }
    }
  }

  protected fun refreshProject() {
    course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.PROJECT_CREATED) ?: error("Cannot find test configurator")
  }

  // TODO: `NavigationUtils.navigateToTask(project, task)` doesn't work with framework lessons
  protected fun getTargetTask(): Task =
    generateSequence(TaskToolWindowView.getInstance(project).currentTask) {
      val nextTask = NavigationUtils.nextTask(it) ?: error("The next task is null")
      NavigationUtils.navigateToTask(project, nextTask, it)
      TaskToolWindowView.getInstance(project).currentTask
    }.firstOrNull { it.name == taskName && it.lesson.name == lessonName } ?: error("Cannot get the target task")

  protected fun getHint(taskProcessor: TaskProcessorImpl, userCode: String? = null): AiAssistantHint =
    ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.WithResult<AiAssistantHint, Exception>(
      project,
      EduCoreBundle.message("action.Educational.NextStepHint.progress.short.text"),
      true
    ) {
      override fun compute(indicator: ProgressIndicator): AiAssistantHint? {
        indicator.isIndeterminate = true
        return runBlockingCancellable {
          withContext(Dispatchers.EDT) {
            val response = AiHintsAssistant.getAssistant(taskProcessor).getHint(userCode).getOrNull()
            response?.codeHint?.value?.let {
              downloadSolution(taskProcessor.task, project, it)
            }
            response
          }
        }
      }
    })

  protected fun runCheck(task: Task, assertAction: (CheckResult) -> Unit) {
    val future = ApplicationManager.getApplication().executeOnPooledThread {
      ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Backgroundable(
        project, EduCoreBundle.message("progress.title.checking.solution"), true
      ) {
        private val checker: TaskChecker<*>

        init {
          val configurator = task.course.configurator
          checker = configurator?.taskCheckerProvider?.getTaskChecker(task, project) ?: error("Cannot find test configurator")
        }

        override fun run(indicator: ProgressIndicator) {
          indicator.text = EduAIBundle.message("validation.test.checking", task.name)
          val checkerResult = checker.check(indicator)
          assertAction(checkerResult)
          println(checkerResult)
          indicator.text = EduAIBundle.message("validation.test.check.result", checkerResult)
        }

      })
    }
    EduActionUtils.waitAndDispatchInvocationEvents(future)
  }

  override fun createCheckerFixture() = JdkCheckerFixture()

  override fun createCourse() = course
}
