package com.jetbrains.edu.learning

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.openapi.ui.TestInputDialog
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.*
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.handlers.UserCreatedFileListener
import com.jetbrains.edu.learning.storage.LearningObjectStorageType
import com.jetbrains.edu.learning.storage.getDefaultLearningObjectsStorageType
import com.jetbrains.edu.learning.storage.pathInStorage
import com.jetbrains.edu.learning.storage.setDefaultLearningObjectsStorageType
import io.mockk.spyk
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import kotlin.test.assertIs

val isTeamCity: Boolean get() = System.getenv("TEAMCITY_VERSION") != null

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
  val e = if (context != null) TestActionEvent.createTestEvent(action, context) else TestActionEvent.createTestEvent(action)

  val project = e.project
  if ((action !is DumbAware || action is CheckAction) && project != null) {
    // Since 242 tests don't wait when indexes are ready.
    // But we don't want to check non-`DumbAware` actions in dumb mode since it doesn't make sense.
    // Also, even `CheckAction` is `DumbAware`, we don't want to run it in dumb mode as well
    // because the only thing that it does in dumb mode is showing balloon to inform users about dumb mode
    IndexingTestUtil.waitUntilIndexesAreReady(project)
  }

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

fun assertContentsEqual(path: String, expectedContents: FileContents, actualContents: FileContents) {
  when (expectedContents) {
    is BinaryContents -> {
      assertIs<BinaryContents>(actualContents, "Contents of $path must be binary")
      assertArrayEquals("Unexpected contents of $path", expectedContents.bytes, actualContents.bytes)
    }
    is TextualContents -> {
      assertIs<TextualContents>(actualContents, "Contents of $path must be textual")
      assertEquals("Unexpected contents of $path", expectedContents.text, actualContents.text)
    }
    is UndeterminedContents -> {
      assertIs<UndeterminedContents>(actualContents, "Contents of $path must be undetermined")
      assertEquals(
        "Unexpected contents of $path",
        expectedContents.textualRepresentation,
        actualContents.textualRepresentation
      )
    }
  }
}

fun assertContentsEqual(task: Task, fileName: String, expectedContents: FileContents) {
  val taskFile = task.taskFiles[fileName] ?: error("Failed to find file $fileName in task $task")
  assertContentsEqual(taskFile.pathInStorage, expectedContents, taskFile.contents)
}

fun assertContentsEqual(task: Task, fileName: String, expectedText: String) =
  assertContentsEqual(task, fileName, InMemoryTextualContents(expectedText))

fun doWithLearningObjectsStorageType(type: LearningObjectStorageType, action: () -> Unit) {
  val savedType = getDefaultLearningObjectsStorageType()
  setDefaultLearningObjectsStorageType(type)

  try {
    action()
  }
  finally {
    setDefaultLearningObjectsStorageType(savedType)
  }
}

@Suppress("HardCodedStringLiteral")
fun simpleDiffRequestChain(
  project: Project,
  currentContentText: String = "Current Content",
  anotherContentText: String = "Another Content"
): SimpleDiffRequestChain {
  val diffContentFactory = DiffContentFactory.getInstance()
  val currentContent = diffContentFactory.create(project, currentContentText)
  val anotherContent = diffContentFactory.create(project, anotherContentText)

  return SimpleDiffRequestChain(
    listOf(
      SimpleDiffRequest(
        "Simple Diff Request", currentContent, anotherContent, "Title: 1", "Title: 2"
      )
    )
  )
}

/**
 * Waits [iterations] number of iterations for [condition] becomes true,
 * processing EDT events on each iteration.
 * If the condition is not satisfied after all iterations, it throws [IllegalStateException]
 *
 * It's supposed to be used to wait for a background process to finish
 */
@RequiresEdt
fun waitFor(iterations: Int = 100, condition: () -> Boolean) {
  repeat(iterations) {
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    if (condition()) return
    Thread.sleep(50)
  }

  error("Too long waiting")
}

/**
 * Temporarily replaces an existing service with a new spy created by [spyk].
 * Behavior of mocked service can be adjusted with [io.mockk.every] API
 *
 * @see [replaceService]
 */
inline fun <reified T : Any> UsefulTestCase.mockService(componentManager: ComponentManager): T {
  val service = componentManager.getService(T::class.java)
  return spyk(service) {
    componentManager.replaceService(T::class.java, this, testRootDisposable)
  }
}
