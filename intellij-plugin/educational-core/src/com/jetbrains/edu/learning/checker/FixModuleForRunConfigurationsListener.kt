package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir

/**
 * Sets up correct module for custom task run configuration if needed.
 *
 * Run configuration can use module to run the corresponding command properly.
 * But serialized form of run configuration keeps only module name and
 * if you create course project, your modules may have different names.
 * As a result, run configuration won't be properly set up by the platform.
 */
class FixModuleForRunConfigurationsListener(private val project: Project) : RunManagerListener, ModuleRootListener {

  private fun learnerMode(): Boolean = project.isEduProject() && project.course?.isStudy == true

  override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
    if (!learnerMode()) return
    updateConfiguration(settings)
  }

  override fun rootsChanged(event: ModuleRootEvent) {
    if (!learnerMode()) return
    RunManager.getInstance(project).allSettings.forEach {
      updateConfiguration(it)
    }
  }

  private fun updateConfiguration(settings: RunnerAndConfigurationSettings) {
    val path = settings.pathIfStoredInArbitraryFileInProject ?: return
    val configuration = settings.configuration as? ModuleBasedConfiguration<*, *> ?: return

    val file = LocalFileSystem.getInstance().findFileByPath(path) ?: return
    if (file.isTaskRunConfigurationFile(project)) {
      // It's essential to search module for src dir because in some cases (Gradle-based projects)
      // modules of task directory and source directory may differ
      when (val result = findSrcDir(file)) {
        is Ok -> configuration.setModule(ModuleUtilCore.findModuleForFile(result.value, project))
        is Err -> LOG.error(result.error)
      }
    }
  }

  private fun findSrcDir(file: VirtualFile): Result<VirtualFile, String> {
    val taskDir = file.getTaskDir(project) ?: return Err("Failed to find task dir for $file")
    val task = taskDir.getContainingTask(project) ?: return Err("Failed to find task for $taskDir")
    val srcDir = task.findSourceDir(taskDir) ?: return Err("Failed to find source dir for ${task.name} task")
    return Ok(srcDir)
  }

  companion object {
    private val LOG: Logger = logger<FixModuleForRunConfigurationsListener>()
  }
}
