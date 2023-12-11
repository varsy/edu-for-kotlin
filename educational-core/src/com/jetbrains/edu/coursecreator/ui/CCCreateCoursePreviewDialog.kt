package com.jetbrains.edu.coursecreator.ui

import com.intellij.CommonBundle
import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages.showErrorDialog
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.Executor
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.JsonTextToSQLiteConverter
import com.jetbrains.edu.learning.courseFormat.copy
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorState
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.VisibleForTesting
import java.awt.event.ActionEvent
import java.io.IOException
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

class CCCreateCoursePreviewDialog(
  private val project: Project,
  course: EduCourse,
  private val configurator: EduConfigurator<*>
) : DialogWrapper(true) {

  @VisibleForTesting
  val panel: CoursePanel = CourseArchivePanel(disposable)

  init {
    title = EduCoreBundle.message("course.creator.create.course.preview.dialog.title")
    setOKButtonText(EduCoreBundle.message("course.creator.create.course.preview.button"))
    panel.preferredSize = JBUI.size(WIDTH, HEIGHT)
    panel.minimumSize = JBUI.size(WIDTH, HEIGHT)
    val courseCopy = course.copy().apply {
      // is set not to show "Edit" button for course preview
      isPreview = true
      // is set to show "Start" button for course preview
      courseMode = CourseMode.STUDENT
    }
    panel.bindCourse(courseCopy)
    init()
  }

  override fun createCenterPanel(): JComponent = panel

  override fun createActions(): Array<out Action> {
    val closeAction = object : AbstractAction(UIUtil.replaceMnemonicAmpersand(CommonBundle.message("button.close"))) {
      override fun actionPerformed(e: ActionEvent) {
        close()
      }
    }

    return arrayOf(closeAction)
  }

  fun close() {
    close(OK_EXIT_CODE)
  }

  override fun getStyle(): DialogStyle {
    return DialogStyle.COMPACT
  }

  @VisibleForTesting
  inner class CourseArchivePanel(parentDisposable: Disposable) : CoursePanel(parentDisposable, false) {

    override fun showError(errorState: ErrorState) {}

    override fun joinCourseAction(info: CourseCreationInfo, mode: CourseMode) {
      createCoursePreview()
    }

    private fun createCoursePreview() {
      val folder = CCUtils.getGeneratedFilesFolder(project)
      if (folder == null) {
        LOG.info(TMP_DIR_ERROR)
        showErrorDialog(project, EduCoreBundle.message("course.creator.create.course.preview.tmpdir.message"),
                        EduCoreBundle.message("course.creator.create.course.preview.failed.title"))
        return
      }
      val courseName = course?.name
      val archiveName = if (courseName.isNullOrEmpty()) EduNames.COURSE else FileUtil.sanitizeFileName(courseName)
      val archiveLocation = "${folder.path}/$archiveName.zip"
      close(OK_EXIT_CODE)
      val errorMessage = CourseArchiveCreator(project, archiveLocation).createArchive()

      if (errorMessage.isNullOrEmpty()) {
        val lastProjectCreationLocation = RecentProjectsManager.getInstance().lastProjectCreationLocation
        try {
          JsonTextToSQLiteConverter().use { jsonTextConverter ->
            val archivePath = FileUtil.join(FileUtil.toSystemDependentName(folder.path), "$archiveName.zip")
            val course = Executor.execCancelable(EduCoreBundle.message("action.create.course.archive.reading.progress.bar")) {
              EduUtilsKt.getLocalCourse(archivePath, jsonTextConverter)
            } as? EduCourse ?: return
            course.isPreview = true

            val location = FileUtil.createTempDirectory(PREVIEW_FOLDER_PREFIX, null)
            val settings = panel.projectSettings ?: error("Project settings shouldn't be null")
            val previewProject = configurator.courseBuilder.getCourseProjectGenerator(course)
              ?.doCreateCourseProject(location.absolutePath, settings)
            if (previewProject == null) {
              LOG.info("Failed to create project for course preview")
              showErrorDialog(project, EduCoreBundle.message("course.creator.create.course.preview.failed.message"),
                              EduCoreBundle.message("course.creator.create.course.preview.failed.title"))
              return
            }
            RecentProjectsManager.getInstance().removePath(location.absolutePath)
            EduCounterUsageCollector.createCoursePreview()
          }
        }
        catch (e: IOException) {
          LOG.info(TMP_DIR_ERROR, e)
          showErrorDialog(project, EduCoreBundle.message("course.creator.create.course.preview.tmpdir.message"),
                          EduCoreBundle.message("course.creator.create.course.preview.failed.title"))
        }
        finally {
          RecentProjectsManager.getInstance().lastProjectCreationLocation = lastProjectCreationLocation
        }
      }
      else {
        LOG.info(errorMessage)
        showErrorDialog(project, errorMessage, EduCoreBundle.message("course.creator.create.course.preview.failed.title"))
      }
    }
  }

  companion object {
    private val LOG: Logger = logger<CCCreateCoursePreviewDialog>()

    private const val WIDTH: Int = 450
    private const val HEIGHT: Int = 500
    private const val TMP_DIR_ERROR = "Failed to create temp directory for course preview"

    const val PREVIEW_FOLDER_PREFIX: String = "course_preview"
  }
}
