@file:Suppress("DEPRECATION")

package com.jetbrains.edu.learning

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.fileContents.InMemoryFileContentsHolder
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.handlers.UserCreatedFileListener
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

inline fun <reified T> nullValue(): Matcher<T> = CoreMatchers.nullValue(T::class.java)

fun <T> withFeature(featureId: String, enabled: Boolean, action: () -> T): T {
  val currentValue = isFeatureEnabled(featureId)
  setFeatureEnabled(featureId, enabled)
  return try {
    action()
  }
  finally {
    setFeatureEnabled(featureId, currentValue)
  }
}

inline fun withTestDialog(dialog: TestDialog, action: () -> Unit) {
  val oldDialog = TestDialogManager.setTestDialog(dialog)
  try {
    action()
  }
  finally {
    UIUtil.dispatchAllInvocationEvents()
    TestDialogManager.setTestDialog(oldDialog)
  }
}

inline fun withTestDialog(dialog: TestInputDialog, action: () -> Unit) {
  val oldDialog = TestDialogManager.setTestInputDialog(dialog)
  try {
    action()
  }
  finally {
    TestDialogManager.setTestInputDialog(oldDialog)
  }
}

fun withNotificationCheck(project: Project, disposable: Disposable, check: (Boolean, String) -> Unit, action: () -> Unit) {
  var notificationShown = false
  var notificationText = ""

  project.messageBus.connect(disposable).subscribe(Notifications.TOPIC, object : Notifications {
    override fun notify(notification: Notification) {
      notificationShown = true
      notificationText = notification.content
    }
  })

  action()
  check(notificationShown, notificationText)
}

inline fun <reified T : AnAction> getActionById(actionId: String): T {
  val action = ActionManager.getInstance().getAction(actionId) ?: error("Failed to find action by `$actionId` id")
  return action as? T ?: error("Action `$actionId` is not `${T::class.java.name}`")
}

fun testAction(
  action: AnAction,
  context: DataContext? = null,
  shouldBeEnabled: Boolean = true,
  shouldBeVisible: Boolean = shouldBeEnabled,
  runAction: Boolean = shouldBeEnabled
): Presentation {
  val e = if (context != null) TestActionEvent(context, action) else TestActionEvent(action)
  action.beforeActionPerformedUpdate(e)
  val presentation = e.presentation

  if (presentation.isEnabled != shouldBeEnabled) {
    val message = if (shouldBeEnabled) {
      "`${action.javaClass.name}` action is not enabled as expected"
    } else {
      "`${action.javaClass.name}` action is not disabled as expected"
    }
    error(message)
  }

  if (presentation.isVisible != shouldBeVisible) {
    val message = if (shouldBeVisible) {
      "`${action.javaClass.name}` action is not visible as expected"
    } else {
      "`${action.javaClass.name}` action is not invisible as expected"
    }
    error(message)
  }

  if (ActionUtil.lastUpdateAndCheckDumb(action, e, true) && runAction) {
    ActionUtil.performActionDumbAwareWithCallbacks(action, e)
  }

  return presentation
}

fun testAction(
  actionId: String,
  context: DataContext? = null,
  shouldBeEnabled: Boolean = true,
  shouldBeVisible: Boolean = shouldBeEnabled,
  runAction: Boolean = shouldBeEnabled
): Presentation {
  val action = getActionById<AnAction>(actionId)
  return testAction(action, context, shouldBeEnabled, shouldBeVisible, runAction)
}

fun initializeCourse(project: Project, course: Course) {
  course.init(false)
  StudyTaskManager.getInstance(project).course = course
}

fun Course.findTask(lessonName: String, taskName: String): Task {
  return getLesson(lessonName)?.getTask(taskName) ?: error("Can't find `$taskName` in `$lessonName`")
}

// TODO: set up more items which are enabled in real course project
// TODO: come up with better name when we set up not only virtual file listeners
inline fun withVirtualFileListener(project: Project, course: Course, disposable: Disposable, action: () -> Unit) {
  val listener = if (course.isStudy) UserCreatedFileListener(project) else CCVirtualFileListener(project, disposable)

  val connection = ApplicationManager.getApplication().messageBus.connect()
  connection.subscribe(VirtualFileManager.VFS_CHANGES, listener)
  try {
    action()
  }
  finally {
    connection.disconnect()
  }
}

fun copyFileContents(item1 : StudyItem, item2: StudyItem) {
  fun copyFileContentsForTasks(item1: Task, item2: Task) {
    for (taskFile1 in item1.taskFiles.values) {
      val taskFile2 = item2.getTaskFile(taskFile1.name)
      taskFile2?.contentsHolder = InMemoryFileContentsHolder(taskFile1.contents)
    }
  }

  if (item1 is Task) {
    copyFileContentsForTasks(item1, item2 as Task)
    return
  }
  if (item1 !is ItemContainer) return
  item2 as ItemContainer

  for (subItem1 in item1.items) {
    val subItem2 = item2.getItem(subItem1.name) ?: continue
    copyFileContents(subItem1, subItem2)
  }
}
