package com.jetbrains.edu.rust

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.coursecreator.courseignore.IgnoringEntry
import com.jetbrains.edu.coursecreator.courseignore.ignoringEntry
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.PluginInfos
import com.jetbrains.edu.learning.pluginVersion
import com.jetbrains.edu.rust.checker.RsTaskCheckerProvider
import org.rust.cargo.CargoConstants
import org.rust.ide.icons.RsIcons
import org.rust.lang.RsConstants
import javax.swing.Icon

// BACKCOMPAT: 2023.2. Drop it
private val BUILD_233 = BuildNumber.fromString("233")!!

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
    get() = RsIcons.RUST

  override fun ignoringEntries(): List<IgnoringEntry> =
    super.ignoringEntries() + listOf(
      // Cargo config file should be included into course even it's located in "hidden" `.cargo` directory
      ignoringEntry(
        "cargo configuration and output",
        """
          !/.cargo
          /.cargo/*
          !/.cargo/${CargoConstants.CONFIG_TOML_FILE}
          !/.cargo/${CargoConstants.CONFIG_FILE}
          ${CargoConstants.LOCK_FILE}
          ${CargoConstants.ProjectLayout.target}/
        """
      )
    )

  private fun VirtualFile.isCargoConfigDirOrFile(project: Project): Boolean {
    val courseDir = project.courseDir
    val cargoDir = courseDir.findChild(".cargo") ?: return false
    if (cargoDir == this) return true
    // Cargo config file should be included into course even it's located in "hidden" directory
    return cargoDir.findChild(CargoConstants.CONFIG_TOML_FILE) == this || cargoDir.findChild(CargoConstants.CONFIG_FILE) == this
  }

  override val isEnabled: Boolean
    get() {
      val rustPluginVersion = pluginVersion(PluginInfos.RUST.stringId) ?: return false
      val currentBuild = ApplicationInfo.getInstance().build
      // Rust plugin changed the signature of `CargoProjectsService.refreshAllProjects` method since `232.23135` and `233.23135` builds.
      // It added default parameter which breaks binary compatibility
      val minSupportedVersion = if (currentBuild < BUILD_233) "232.23135" else "233.23135"
      return VersionComparatorUtil.compare(rustPluginVersion, minSupportedVersion) >= 0
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

}
