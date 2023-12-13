package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.courseFormat.JsonTextToSQLiteConverter
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CCUnpackCourseArchive : DumbAwareAction() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = !RemoteEnvHelper.isRemoteDevServer()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val descriptor = FileChooserDescriptor(true, true, true, true,
      true, false)
    val virtualFile = FileChooser.chooseFile(descriptor, project, null) ?: return
    val jsonTextConverter = JsonTextToSQLiteConverter()
    val course = EduUtilsKt.getLocalCourse(virtualFile.path, jsonTextConverter)

    if (course == null) {
      Messages.showErrorDialog(
        EduCoreBundle.message("dialog.message.course.incompatible"),
        EduCoreBundle.message("dialog.title.failed.to.unpack.course")
      )
      jsonTextConverter.close()
      return
    }

    val newCourseDialog = CCNewCourseDialog(
      EduCoreBundle.message("dialog.title.unpack.course"),
      EduCoreBundle.message("button.unpack"),
      course,
      courseTexts = jsonTextConverter
    )
    newCourseDialog.show()
  }
}
