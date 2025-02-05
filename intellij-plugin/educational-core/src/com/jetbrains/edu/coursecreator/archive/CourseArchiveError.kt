package com.jetbrains.edu.coursecreator.archive

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.coursecreator.actions.marketplace.RegenerateDuplicateIds
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.marketplace.DuplicateIdMap
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import org.jetbrains.annotations.Nls
import java.io.FileNotFoundException

interface CourseArchiveError {

  val message: @NotificationContent String

  /**
   * Creates notification for particular error.
   * It's supposed to be overridden if the notification requires additional custom elements:
   * actions, notification listeners, etc.
   *
   * See [showNotification]
   */
  fun notification(@NotificationTitle title: String): Notification {
    return EduNotificationManager.create(NotificationType.ERROR, title, message)
  }

  /**
   * Action which is supposed to be performed without additional user actions
   */
  @RequiresEdt
  fun immediateAction(project: Project) {}
}

/**
 * Shows notification with specified [title] associated with particular [CourseArchiveError]
 */
fun CourseArchiveError.showNotification(project: Project, @NotificationTitle title: String) {
  notification(title).notify(project)
}

abstract class ExceptionCourseArchiveError<T : Throwable>(val exception: T) : CourseArchiveError {
  override val message: String
    get() = exception.message.orEmpty()
}

class HugeBinaryFileError(e: HugeBinaryFileException) : ExceptionCourseArchiveError<HugeBinaryFileException>(e)
class BrokenPlaceholderError(e: BrokenPlaceholderException) : ExceptionCourseArchiveError<BrokenPlaceholderException>(e) {
  override fun immediateAction(project: Project) {
    val yamlFile = exception.placeholder.taskFile.task.getDir(project.courseDir)?.findChild(TASK_CONFIG) ?: return
    FileEditorManager.getInstance(project).openFile(yamlFile, true)
  }
}
// TODO: use more specific exception for error related to additional files.
//  `FileNotFoundException` is not related to additional files
//  and in theory may occur in other cases as well
class AdditionalFileNotFoundError(e: FileNotFoundException) : ExceptionCourseArchiveError<FileNotFoundException>(e)
class OtherError(e: Throwable, private val errorMessage: @Nls String? = null) : ExceptionCourseArchiveError<Throwable>(e) {
  override val message: String
    get() = errorMessage ?: EduCoreBundle.message("error.failed.to.create.course.archive.notification.title")
}

data class DuplicateIdsError(val items: DuplicateIdMap) : CourseArchiveError {
  override val message: String
    get() {
      val htmlItemList = buildString {
        appendLine("<ul>")
        for (itemsWithSameId in items.values) {
          // TODO: add links to the corresponding config
          appendLine(itemsWithSameId.joinToString(", ", prefix = "<li>", postfix = "</li>") { it.presentableName })
        }
        appendLine("</ul>")
      }

      return EduCoreBundle.message("error.failed.to.create.course.archive.duplicate.ids.message", htmlItemList)
    }

  override fun notification(title: String): Notification {
    return super.notification(title)
      .addAction(ActionManager.getInstance().getAction(RegenerateDuplicateIds.ACTION_ID))
  }
}
