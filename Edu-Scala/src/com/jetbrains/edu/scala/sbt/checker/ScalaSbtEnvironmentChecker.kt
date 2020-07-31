package com.jetbrains.edu.scala.sbt.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreErrorBundle
import com.jetbrains.edu.scala.messages.EduScalaBundle
import com.jetbrains.edu.scala.sbt.isSbtProject

class ScalaSbtEnvironmentChecker: EnvironmentChecker() {
  override fun checkEnvironment(project: Project, task: Task): String? {
    if (ProjectRootManager.getInstance(project).projectSdk == null) {
      return EduCoreErrorBundle.message("no.sdk")
    }
    if (!project.isSbtProject) {
      return EduScalaBundle.message("error.no.sbt.project")
    }
    return null
  }
}