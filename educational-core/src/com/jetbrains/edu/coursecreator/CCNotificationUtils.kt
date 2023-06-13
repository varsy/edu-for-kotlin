package com.jetbrains.edu.coursecreator

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.util.NlsContexts.NotificationTitle
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.messages.EduCoreBundle

object CCNotificationUtils {
  private val LOG = Logger.getInstance(CCNotificationUtils::class.java)

  val showLogAction: AnAction
    get() = ActionManager.getInstance().getAction("ShowLog")

  fun showFailedToPostItemNotification(project: Project, item: StudyItem, isNew: Boolean) {
    val pathInCourse = item.getPathInCourse()

    val title = if (isNew) EduCoreBundle.message("notification.course.creator.failed.to.upload.item.title")
    else EduCoreBundle.message("notification.course.creator.failed.to.update.item.title")

    val content = if (isNew) EduCoreBundle.message("notification.course.creator.failed.to.upload.item.content", pathInCourse)
    else EduCoreBundle.message("notification.course.creator.failed.to.update.item.content", pathInCourse)

    showErrorNotification(project, title, content, showLogAction)
  }

  fun showErrorNotification(
    project: Project,
    @NotificationTitle title: String,
    @NotificationContent message: String? = null,
    action: AnAction? = null
  ) {
    LOG.info(message)
    val notification = Notification("JetBrains Academy", title, message.orEmpty(), NotificationType.ERROR)
    if (action != null) {
      notification.addAction(action)
    }
    notification.notify(project)
  }

  fun showNoRightsToUpdateOnStepikNotification(project: Project) {
    showErrorNotification(
      project,
      EduCoreBundle.message("notification.course.creator.access.denied.title"),
      EduCoreBundle.message("notification.course.creator.access.denied.content")
    )
  }

  fun showNotification(
    project: Project,
    @NotificationContent title: String,
    action: AnAction? = null
  ) {
    showNotification(project, action, title, "")
  }

  fun showNotification(
    project: Project,
    @NotificationTitle title: String,
    @NotificationContent message: String,
    action: AnAction? = null
  ) {
    showNotification(project, action, title, message)
  }

  fun showNotification(
    project: Project,
    action: AnAction?,
    @NotificationTitle title: String,
    @NotificationContent message: String,
    notificationType: NotificationType = NotificationType.INFORMATION
  ) {
    val notification = Notification("JetBrains Academy", title, message, notificationType)
    if (action != null) {
      notification.addAction(action)
    }
    notification.notify(project)
  }

  fun showLoginSuccessfulNotification(userName: String) {
    Notification(
      "JetBrains Academy",
      EduCoreBundle.message("login.successful"),
      EduCoreBundle.message("logged.in.as", userName),
      NotificationType.INFORMATION
    ).notify(null)
  }
}
