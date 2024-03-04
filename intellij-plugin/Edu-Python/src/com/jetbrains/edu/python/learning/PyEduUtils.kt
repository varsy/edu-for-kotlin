@file:JvmName("PyEduUtils")
package com.jetbrains.edu.python.learning

import com.intellij.execution.ExecutionException
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.impl.NotificationsConfigurationImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.courseignore.ignoringEntry
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.courseFormat.ext.findTaskFileInDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isTestsFile
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings
import com.jetbrains.python.packaging.PyPackageManagerUI
import com.jetbrains.python.packaging.PyPackageUtil
import com.jetbrains.python.psi.LanguageLevel

fun Task.getCurrentTaskVirtualFile(project: Project): VirtualFile? {
  val taskDir = getDir(project.courseDir) ?: error("Failed to get task dir for `${name}` task")
  var resultFile: VirtualFile? = null
  for ((_, taskFile) in taskFiles) {
    val file = taskFile.findTaskFileInDir(taskDir) ?: continue
    if (file.isTestsFile(project) || !TextEditorProvider.isTextFile(file)) continue
    if (resultFile == null) {
      resultFile = file
    }

    val hasNewPlaceholder = taskFile.answerPlaceholders.any { p -> p.placeholderDependency == null }
    if (hasNewPlaceholder) return file
  }
  return resultFile
}

fun Task.getCurrentTaskFilePath(project: Project): String? {
  return getCurrentTaskVirtualFile(project)?.systemDependentPath
}

fun pyIgnoringEntries() = listOf(
  ignoringEntry(
    "Python",
    FOLDERS_TO_EXCLUDE.joinToString("\n") { "$it/" } + """
      *.pyc
    """
  )
)

fun installRequiredPackages(project: Project, sdk: Sdk) {
  for (module in ModuleManager.getInstance(project).modules) {
    val requirements = runReadAction { PyPackageUtil.getRequirementsFromTxt(module) }
    if (requirements.isNullOrEmpty()) {
      continue
    }
    PyPackageManagerUI(project, sdk, object : PyPackageManagerUI.Listener {
      override fun started() {}
      override fun finished(list: List<ExecutionException>) {
        disableSuccessfulNotification(list)
      }

      private fun disableSuccessfulNotification(list: List<ExecutionException>) {
        if (list.isNotEmpty()) {
          return
        }
        val notificationsConfiguration = NotificationsConfigurationImpl.getInstanceImpl()
        val oldSettings = NotificationsConfigurationImpl.getSettings(PY_PACKAGES_NOTIFICATION_GROUP)
        notificationsConfiguration.changeSettings(
          PY_PACKAGES_NOTIFICATION_GROUP,
          NotificationDisplayType.NONE, true, false
        )

        // IDE will try to show notification after listener's `finished` in invokeLater
        ApplicationManager.getApplication().invokeLater {
          notificationsConfiguration.changeSettings(
            PY_PACKAGES_NOTIFICATION_GROUP,
            oldSettings.displayType, oldSettings.isShouldLog,
            oldSettings.isShouldReadAloud
          )
        }
      }
    }).install(requirements, emptyList())
  }
}

fun getSupportedVersions(): List<String> {
  val pythonVersions = mutableListOf(PyLanguageSettings.ALL_VERSIONS, PYTHON_3_VERSION, PYTHON_2_VERSION)
  pythonVersions.addAll(LanguageLevel.values().map { it.toString() }.reversed())
  return pythonVersions
}


private val VirtualFile.systemDependentPath: String get() = FileUtil.toSystemDependentName(path)

private val FOLDERS_TO_EXCLUDE: List<String> = listOf("__pycache__", "venv")

// should be the same as [PyPackageManagerUI.PackagingTask.PACKAGING_GROUP_ID]
private const val PY_PACKAGES_NOTIFICATION_GROUP = "Packaging"

