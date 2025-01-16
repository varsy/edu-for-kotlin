package com.jetbrains.edu.jvm.slow.checker

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.IdeaTestUtil
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.isTeamCity
import java.io.File

class JdkCheckerFixture : EduCheckerFixture<JdkProjectSettings>() {

  private var sdks: Set<Sdk>? = null

  override val projectSettings: JdkProjectSettings get() {
    val jdk = ProjectJdkTable.getInstance().findJdk(MY_TEST_JDK_NAME) ?: error("Gradle JDK should be configured in setUp()")

    val sdksModel = ProjectSdksModel()
    sdksModel.addSdk(jdk)

    return JdkProjectSettings(sdksModel, jdk)
  }

  override fun getSkipTestReason(): String? {
    // We temporarily disable checkers tests on teamcity linux agents
    // because they don't work on these agents and we can't find out a reason :((
    return if (SystemInfo.isLinux && isTeamCity) "Linux TeamCity agent" else super.getSkipTestReason()
  }

  override fun setUp() {
    val myJdkHome = IdeaTestUtil.requireRealJdkHome()
    VfsRootAccess.allowRootAccess(testRootDisposable, myJdkHome)

    sdks = ProjectJdkTable.getInstance().allJdks.toSet()

    val jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(myJdkHome))!!
    val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), jdkHomeDir, JavaSdk.getInstance(), true, null, MY_TEST_JDK_NAME)
    checkNotNull(jdk) {
      "Cannot create JDK for $myJdkHome"
    }
    SdkConfigurationUtil.addSdk(jdk)
  }

  override fun tearDown() {
    for (sdk in ProjectJdkTable.getInstance().allJdks) {
      if (sdk !in sdks.orEmpty()) {
        SdkConfigurationUtil.removeSdk(sdk)
      }
    }
    sdks = null
  }

  companion object {
    private const val MY_TEST_JDK_NAME = "Test JDK"
  }
}
