package com.jetbrains.edu.learning.marketplace

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.copyFileContents
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.copy
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.stepik.hyperskill.FrameworkLessonsUpdateTest
import com.jetbrains.edu.learning.testAction
import kotlin.test.assertNotEquals

class MarketplaceFrameworkLessonsUpdateTest : FrameworkLessonsUpdateTest<EduCourse>() {

  fun `test check current task index and task record saved`() {
    createCourseWithFrameworkLessons()

    val task1 = localCourse.taskList[0]

    withVirtualFileListener(localCourse) {
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)
    }

    val taskRecordIndexBeforeUpdate = task1.record

    assertEquals(1, getFirstLesson()?.currentTaskIndex)
    assertNotEquals(-1, taskRecordIndexBeforeUpdate)

    val taskText = "fun foo2() {}"
    val testText = """
      fun test1() {}
      fun test2() {}
    """.trimIndent()
    updateCourse {
      taskList[1].apply {
        taskFiles["src/Task.kt"]!!.text = taskText
        taskFiles["test/Tests2.kt"]!!.text = testText
      }
    }

    val task = localCourse.taskList[1]
    assertEquals(taskText, task.taskFiles["src/Task.kt"]!!.text)
    assertEquals(testText, task.taskFiles["test/Tests2.kt"]!!.text)
    assertEquals(1, getFirstLesson()?.currentTaskIndex)
    assertEquals("Record index should be preserved after update", taskRecordIndexBeforeUpdate, localCourse.taskList[0].record)

    fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt", taskText)
            file("Baz.kt", "fun baz() {}")
          }
          dir("test") {
            file("Tests2.kt", testText)
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  override fun updateCourse(changeCourse: EduCourse.() -> Unit) {
    val remoteCourse = toRemoteCourse(changeCourse)
    MarketplaceCourseUpdater(project, localCourse, remoteCourse.marketplaceCourseVersion).updateCourseWithRemote(remoteCourse)
    assertEquals(remoteCourse.marketplaceCourseVersion, localCourse.marketplaceCourseVersion)
  }

  override fun createCourseWithFrameworkLessons() {
    val eduCourse = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::EduCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun test1() {}")

        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun test2() {}")
        }
      }
    } as EduCourse
    eduCourse.marketplaceCourseVersion = 1
    localCourse = eduCourse
  }

  override fun toRemoteCourse(changeCourse: EduCourse.() -> Unit): EduCourse {
    val remoteCourse = localCourse.copy()
    copyFileContents(localCourse, remoteCourse)
    remoteCourse.init(false)
    remoteCourse.changeCourse()
    return remoteCourse
  }

  private fun getFirstLesson(): FrameworkLesson? = (localCourse.lessons[0] as? FrameworkLesson)
}