package com.jetbrains.edu.rust

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.configuration.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.rust.checker.RsTaskCheckerProvider
import org.rust.cargo.CargoConstants
import org.rust.lang.RsConstants
import javax.swing.Icon

private val BUILD_242: BuildNumber = BuildNumber.fromString("242")!!

class RsConfigurator : EduConfigurator<RsProjectSettings> {
  override val taskCheckerProvider: RsTaskCheckerProvider
    get() = RsTaskCheckerProvider()

  override val testFileName: String
    get() = ""

  override fun getMockFileName(course: Course, text: String): String = RsConstants.MAIN_RS_FILE

  override val courseBuilder: EduCourseBuilder<RsProjectSettings>
    get() = RsCourseBuilder()

  override val testDirs: List<String>
    get() = listOf("tests")

  override val sourceDir: String
    get() = "src"

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Rust

  override fun courseFileAttributes(): AttributesEvaluator = AttributesEvaluator(super.courseFileAttributes()) {
    dir(".cargo") {
      name(CargoConstants.CONFIG_TOML_FILE, CargoConstants.CONFIG_FILE) {
        includeIntoArchive()
      }
      excludeFromArchive()
    }

    path(CargoConstants.ProjectLayout.target) {
      excludeFromArchive()
    }

    name(CargoConstants.LOCK_FILE) {
      excludeFromArchive()
    }
  }

  override val isEnabled: Boolean
    get() {
      val rustPluginVersion = pluginVersion(PluginInfos.RUST.stringId) ?: return false
      val currentBuild = ApplicationInfo.getInstance().build
      // Rust plugin changed `RsToolchainPathChoosingComboBox` API since `241.27011.175`.
      // Also, since `242.23726` `org.rust.lang.core.psi.ext.containingCargoTarget` was changed as well.
      // Let's avoid runtime error because of this binary incompatibility
      val minSupportedVersion = if (currentBuild < BUILD_242) "241.27011.175" else "242.23726"
      return VersionComparatorUtil.compare(rustPluginVersion, minSupportedVersion) >= 0
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

}
