package com.jetbrains.edu.learning.actions

import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.vfs.readText
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.rd.util.first
import org.junit.Test

class ApplyCodeActionTest : EduActionTestCase() {

  @Test
  fun `test apply code not visible and not enabled by default`() {
    testAction(ApplyCodeAction.ACTION_ID, context = null, shouldBeEnabled = false, shouldBeVisible = false)
  }

  @Test
  fun `test apply code not visible and not enabled with wrong context`() {
    testAction(
      ApplyCodeAction.ACTION_ID, context = dataContext(
        ChainDiffVirtualFile(
          simpleDiffRequestChain(project), "Regular Chain Diff Virtual File"
        )
      ), shouldBeEnabled = false, shouldBeVisible = false
    )
  }

  @Test
  fun `test apply code with empty list of virtual file path`() {
    val diffChain = simpleDiffRequestChain(project).apply {
      putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf())
    }
    val diffVirtualFile = ChainDiffVirtualFile(diffChain, "")
    val dataContext =
      SimpleDataContext.builder().add(CommonDataKeys.VIRTUAL_FILE, diffVirtualFile).add(CommonDataKeys.PROJECT, project).build()

    withNotificationCheck(project, testRootDisposable, { notificationShown, notificationText ->
      assertTrue(notificationShown)
      assertEquals(EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.failed.text"), notificationText)
    }) {
      withTestDialog(TestDialog.YES) {
        testAction(ApplyCodeAction.ACTION_ID, context = dataContext)
      }
    }
  }

  @Test
  fun `test apply code with list of wrong virtual file path`() {
    val diffChain = simpleDiffRequestChain(project).apply {
      putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf("src/wrong/path/nonExistingFile.kt"))
    }
    val diffVirtualFile = ChainDiffVirtualFile(diffChain, "")
    val dataContext =
      SimpleDataContext.builder().add(CommonDataKeys.VIRTUAL_FILE, diffVirtualFile).add(CommonDataKeys.PROJECT, project).build()

    withNotificationCheck(project, testRootDisposable, { notificationShown, notificationText ->
      assertTrue(notificationShown)
      assertEquals(EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.failed.text"), notificationText)
    }) {
      withTestDialog(TestDialog.YES) {
        testAction(ApplyCodeAction.ACTION_ID, context = dataContext)
      }
    }
  }

  @Test
  fun `test apply code`() {
    val course = courseWithFiles {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("taskFile1.txt", CURRENT_CONTENT)
        }
      }
    }
    val taskVirtualFile = course.allTasks.first().taskFiles.first().value.getVirtualFile(project) ?: error("No virtual file")
    val diffChain = simpleDiffRequestChain(project, CURRENT_CONTENT, NEW_CONTENT).apply {
      putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf(taskVirtualFile.path))
    }
    val diffVirtualFile = ChainDiffVirtualFile(diffChain, "Diff")
    val dataContext =
      SimpleDataContext.builder().add(CommonDataKeys.VIRTUAL_FILE, diffVirtualFile).add(CommonDataKeys.PROJECT, project).build()

    withNotificationCheck(project, testRootDisposable, { notificationShown, notificationText ->
      assertTrue(notificationShown)
      assertEquals(EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.success.text"), notificationText)
    }) {
      withTestDialog(TestDialog.YES) {
        testAction(ApplyCodeAction.ACTION_ID, context = dataContext)
      }
    }

    val actualText = course.allTasks.first().taskFiles.first().value.getVirtualFile(project)?.readText()
    assertEquals(NEW_CONTENT, actualText)
  }

  companion object {
    private const val CURRENT_CONTENT = "Some Content"
    private const val NEW_CONTENT = "New Content"
  }
}
