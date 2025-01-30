package com.jetbrains.edu.aiHints.core

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.actions.EduAIHintsUtils
import com.jetbrains.edu.learning.actions.EduAIHintsUtils.GET_HINT_ACTION_ID
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.submissions.UserAgreementState
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class GetHintTest : EduActionTestCase() {
  @Test
  fun `Get Hint action unavailable by default`() {
    plainTextCourseWithFile()
    testGetHintAction()
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isAvailable())
  }

  @Test
  fun `GetHint action unavailable for non-marketplace student course`() {
    acceptAgreement()
    plainTextCourseWithFile()
    testGetHintAction()
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isAvailable())
  }

  @Test
  fun `GetHint action available for marketplace student course with edu task`() {
    acceptAgreement()
    plainTextCourseWithFile().apply {
      id = 1
      isMarketplace = true
    }
    testGetHintAction()
    assertFalse(EduAIHintsUtils.getHintActionPresentation(project).isAvailable())
  }

  @Test
  fun `GetHint action available`() {
    acceptAgreement()
    val course = plainTextCourseWithFile().apply {
      id = 1
      isMarketplace = true
    }
    val task = course.findTask("lesson1", "task1")
    task.status = CheckStatus.Failed
    val taskVirtualFile = task.getTaskFile("task.txt")?.getVirtualFile(project) ?: error("Virtual File for task is not found")
    FileEditorManager.getInstance(project).openFile(taskVirtualFile, true)
    testGetHintAction(shouldBeEnabled = true)
    assertTrue(EduAIHintsUtils.getHintActionPresentation(project).isAvailable())
  }

  private fun plainTextCourseWithFile(): EduCourse = courseWithFiles {
    lesson("lesson1") {
      eduTask("task1") {
        taskFile("task.txt")
      }
    }
  } as EduCourse

  private fun acceptAgreement() {
    UserAgreementSettings.getInstance().setAgreementState(
      UserAgreementSettings.AgreementStateResponse(
        UserAgreementState.ACCEPTED,
        UserAgreementState.ACCEPTED
      )
    )
  }

  private fun testGetHintAction(shouldBeEnabled: Boolean = false) {
    val action = ActionManager.getInstance().getAction(GET_HINT_ACTION_ID)
    testAction(action, shouldBeEnabled = shouldBeEnabled, runAction = false)
  }
}