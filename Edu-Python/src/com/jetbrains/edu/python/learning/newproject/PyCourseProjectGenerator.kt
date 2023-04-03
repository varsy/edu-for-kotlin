package com.jetbrains.edu.python.learning.newproject

import com.intellij.execution.ExecutionException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createTextChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.installRequiredPackages
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings.Companion.getBaseSdk
import com.jetbrains.edu.python.learning.newproject.PyLanguageSettings.Companion.installSdk
import com.jetbrains.edu.python.learning.newproject.PySdkSettingsHelper.Companion.firstAvailable
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PySdkToInstall
import com.jetbrains.python.sdk.associateWithModule
import com.jetbrains.python.sdk.createSdkByGenerateTask
import java.io.IOException

open class PyCourseProjectGenerator(
  builder: EduCourseBuilder<PyProjectSettings>,
  course: Course
) : CourseProjectGenerator<PyProjectSettings>(builder, course) {
  @Throws(IOException::class)
  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    val testHelper = EduNames.TEST_HELPER
    if (holder.courseDir.findChild(testHelper) != null) return
    val templateText = getInternalTemplateText("test_helper")
    createTextChildFile(holder, holder.courseDir, testHelper, templateText, true)
  }

  override fun afterProjectGenerated(project: Project, projectSettings: PyProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    var sdk = projectSettings.sdk
    if (sdk is PySdkToInstall) {
      val selectedSdk = sdk
      ApplicationManager.getApplication().invokeAndWait {
        installSdk(selectedSdk)
      }
      createAndAddVirtualEnv(project, projectSettings)
      sdk = projectSettings.sdk
    }
    if (sdk?.sdkType === PyFakeSdkType) {
      createAndAddVirtualEnv(project, projectSettings)
      sdk = projectSettings.sdk
    }
    sdk = updateSdkIfNeeded(project, sdk)
    SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
    if (sdk == null) {
      return
    }
    installRequiredPackages(project, sdk)
  }

  private fun createAndAddVirtualEnv(project: Project, settings: PyProjectSettings) {
    val course = StudyTaskManager.getInstance(project).course ?: return
    val baseSdkPath = getBaseSdkPath(settings, course) ?: return
    val baseSdk = PyDetectedSdk(baseSdkPath)
    val virtualEnvPath = project.basePath + "/.idea/VirtualEnvironment"
    val sdk = createSdkByGenerateTask(object : Task.WithResult<String, ExecutionException>(
      project,
      message("creating.virtual.environment"),
      false
    ) {
      @Throws(ExecutionException::class)
      override fun compute(indicator: ProgressIndicator): String {
        indicator.isIndeterminate = true
        val packageManager = PyPackageManager.getInstance(baseSdk)
        return packageManager.createVirtualEnv(virtualEnvPath, false)
      }
    }, allSdks, baseSdk, project.basePath, null)
    if (sdk == null) {
      LOG.warn("Failed to create virtual env in $virtualEnvPath")
      return
    }
    settings.sdk = sdk
    SdkConfigurationUtil.addSdk(sdk)
    sdk.associateWithModule(null, project.basePath)
  }

  companion object {
    private val LOG = logger<PyCourseProjectGenerator>()

    private fun getBaseSdkPath(settings: PyProjectSettings, course: Course): String? {
      if (isUnitTestMode) {
        val sdk = settings.sdk
        return sdk?.homePath
      }
      val baseSdk = getBaseSdk(course)
      return baseSdk?.path
    }

    private fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
      val helper = firstAvailable()
      return helper.updateSdkIfNeeded(project, sdk)
    }

    private val allSdks: List<Sdk>
      get() {
        val helper = firstAvailable()
        return helper.getAllSdks()
      }
  }
}
