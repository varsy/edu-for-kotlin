package com.jetbrains.edu.rust

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.configuration.ArchiveFileInfo
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.buildArchiveFileInfo
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

  override fun archiveFileInfo(holder: CourseInfoHolder<out Course?>, file: VirtualFile): ArchiveFileInfo =
    buildArchiveFileInfo(holder, file) {
      when {
        file.isCargoConfigDirOrFile(holder.courseDir) -> {
          description("Cargo configuration files")
          includeInArchive()
        }

        nameRegex("^${CargoConstants.LOCK_FILE}$") -> {
          description("Cargo lock file")
        }

        regex("(/|^)${CargoConstants.ProjectLayout.target}(/|$)") -> {
          description("Build output file")
        }

        else -> info(super.archiveFileInfo(holder, file))
      }
    }

  private fun VirtualFile.isCargoConfigDirOrFile(courseDir: VirtualFile): Boolean {
    val cargoDir = courseDir.findChild(".cargo") ?: return false
    if (cargoDir == this) return true
    // Cargo config file should be included into course even it's located in "hidden" directory
    return cargoDir.findChild(CargoConstants.CONFIG_TOML_FILE) == this || cargoDir.findChild(CargoConstants.CONFIG_FILE) == this
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
