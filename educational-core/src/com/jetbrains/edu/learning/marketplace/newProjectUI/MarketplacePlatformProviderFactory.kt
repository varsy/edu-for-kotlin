package com.jetbrains.edu.learning.marketplace.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.io.ZipUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.ToEmptyTextConverter
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.loadMarketplaceCourseStructure
import com.jetbrains.edu.learning.marketplace.updateFeaturedStatus
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import javax.swing.Icon

class MarketplacePlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(MarketplacePlatformProvider())
}

class MarketplacePlatformProvider : CoursesPlatformProvider() {
  private val bundledCoursesNames = listOf("Kotlin Koans.zip", "Introduction to Python.zip")

  override val name: String
    get() = MARKETPLACE

  override val icon: Icon get() = EducationalCoreIcons.MARKETPLACE_TAB

  override fun createPanel(scope: CoroutineScope, disposable: Disposable): CoursesPanel = MarketplaceCoursesPanel(this, scope, disposable)

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    val marketplaceCourses = MarketplaceConnector.getInstance().searchCourses()

    val marketplaceCourseNames = mutableSetOf<String>()
    for (course in marketplaceCourses) {
      marketplaceCourseNames += course.name
      course.updateFeaturedStatus()
    }

    val bundledCourses = loadBundledCourses().filter { bundled ->
      bundled.name !in marketplaceCourseNames
    }

    val courses = (bundledCourses + marketplaceCourses).sortedBy { it.visibility }
    return CoursesGroup(courses).asList()
  }

  override fun joinAction(courseInfo: CourseCreationInfo, courseMode: CourseMode, coursePanel: CoursePanel) {
    courseInfo.course.loadMarketplaceCourseStructure()
    super.joinAction(courseInfo, courseMode, coursePanel)
  }

  private fun loadBundledCourses(): List<Course> {
    val courses = mutableListOf<Course>()
    for (path in getBundledCoursesPaths()) {
      val localCourse = EduUtilsKt.getLocalCourse(path, ToEmptyTextConverter)
      if (localCourse == null) {
        LOG.error("Failed to import local course form $path")
        continue
      }
      localCourse.visibility = CourseVisibility.FeaturedVisibility(BUNDLED_GROUP_ID)
      courses.add(localCourse)
    }
    return courses
  }

  private fun getBundledCoursesPaths(): List<String> {
    return bundledCoursesNames.map { FileUtil.join(getBundledCourseRoot(it, javaClass).absolutePath, it) }
  }

  private fun getBundledCourseRoot(courseName: String, clazz: Class<*>): File {
    @NonNls val jarPath = PathUtil.getJarPathForClass(clazz)
    if (jarPath.endsWith(".jar")) {
      val jarFile = File(jarPath)
      val pluginBaseDir = jarFile.parentFile
      val coursesDir = File(pluginBaseDir, "courses")

      if (!coursesDir.exists()) {
        if (!coursesDir.mkdir()) {
          LOG.info("Failed to create courses dir")
          return coursesDir
        }
      }
      try {
        ZipUtil.extract(jarFile, pluginBaseDir) { _, name -> name == courseName }
      }
      catch (e: IOException) {
        LOG.info("Failed to extract default course", e)
      }
      return coursesDir
    }
    return File(jarPath, "courses")
  }


  companion object {
    private val LOG = Logger.getInstance(MarketplacePlatformProvider::class.java)

    // These ids are used only for sorting
    // Probably, we should use separate id for each course to provide desired sorting
    private const val BUNDLED_GROUP_ID = 0
    const val MARKETPLACE_GROUP_ID = 1
  }
}