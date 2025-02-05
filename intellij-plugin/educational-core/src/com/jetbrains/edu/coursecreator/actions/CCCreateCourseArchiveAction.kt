package com.jetbrains.edu.coursecreator.actions

import com.intellij.ide.actions.RevealFileAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCNotificationUtils
import com.jetbrains.edu.coursecreator.CCUtils.askToWrapTopLevelLessons
import com.jetbrains.edu.coursecreator.CCUtils.isCourseCreator
import com.jetbrains.edu.coursecreator.archive.CourseArchiveCreator
import com.jetbrains.edu.coursecreator.archive.showNotification
import com.jetbrains.edu.coursecreator.ui.CCCreateCourseArchiveDialog
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.courseFormat.ext.hasTopLevelLessons
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.NonNls
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.invariantSeparatorsPathString

class CCCreateCourseArchiveAction : AnAction(EduCoreBundle.lazyMessage("action.create.course.archive.text")) {

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    val project = e.project
    presentation.isEnabledAndVisible = project != null && isCourseCreator(project)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return

    if (course.hasSections && course.hasTopLevelLessons && !askToWrapTopLevelLessons(project, (course as EduCourse))) {
      return
    }

    val (locationPath, authorName, checkAllTasksFlag) = showCourseArchiveDialog(project, course) ?: return
    if (checkAllTasksFlag) {
      ProgressManager.getInstance().run(CheckAllTasksBeforeCreateCourseArchiveProgressTask(project, course) {
        createSourceArchive(project, course, locationPath, authorName)
      })
    }
    else {
      createSourceArchive(project, course, locationPath, authorName)
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun createSourceArchive(project: Project, course: Course, locationPath: Path, authorName: String) {
    course.vendor = Vendor(authorName)
    PropertiesComponent.getInstance(project).setValue(AUTHOR_NAME, authorName)

    val error = CourseArchiveCreator(project, locationPath).createArchive(course)
    if (error == null) {
      CCNotificationUtils.showInfoNotification(
        project,
        EduCoreBundle.message("action.create.course.archive.success.message"),
        action = ShowFileAction(locationPath)
      )
      PropertiesComponent.getInstance(project).setValue(LAST_ARCHIVE_LOCATION, locationPath.invariantSeparatorsPathString)
      EduCounterUsageCollector.createCourseArchive()
    }
    else {
      error.showNotification(project, EduCoreBundle.message("error.failed.to.create.course.archive.notification.title"))
    }
  }

  private fun showCourseArchiveDialog(project: Project, course: Course): Triple<Path, String, Boolean>? {
    val dialog = CCCreateCourseArchiveDialog(project, course.name)
    if (!dialog.showAndGet()) {
      return null
    }
    return Triple(Paths.get(dialog.locationPath), dialog.authorName, dialog.checkAllTasksFlag)
  }

  class ShowFileAction(val path: Path) : AnAction(
    EduCoreBundle.message("action.create.course.archive.open.file", RevealFileAction.getFileManagerName())
  ) {
    override fun actionPerformed(e: AnActionEvent) {
      RevealFileAction.openFile(path)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
      val presentation = e.presentation
      presentation.isVisible = RevealFileAction.isSupported()
    }
  }

  companion object {
    @NonNls
    const val LAST_ARCHIVE_LOCATION = "Edu.CourseCreator.LastArchiveLocation"

    @NonNls
    const val AUTHOR_NAME = "Edu.Author.Name"
  }
}
