@file:JvmName("OpenProjectUtils")

package com.jetbrains.edu.learning.newproject

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.project.Project
import com.intellij.platform.PlatformProjectOpenProcessor
import com.intellij.projectImport.ProjectOpenedCallback
import java.nio.file.Path

fun openNewProject(location: Path, callback: ProjectOpenedCallback): Project? {
  val task = OpenProjectTask(
    forceOpenInNewFrame = true,
    projectToClose = null,
    isNewProject = true,
    callback = callback
  )

  @Suppress("UnstableApiUsage")
  return PlatformProjectOpenProcessor.doOpenProject(location, task)
}
