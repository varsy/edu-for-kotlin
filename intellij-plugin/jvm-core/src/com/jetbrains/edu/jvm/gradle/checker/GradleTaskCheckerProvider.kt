package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

open class GradleTaskCheckerProvider : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = GradleCodeExecutor()

  override val envChecker: EnvironmentChecker
    get() = GradleEnvironmentChecker()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
    NewGradleEduTaskChecker(task, envChecker, project)
}
