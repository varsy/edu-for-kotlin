package com.jetbrains.edu.learning.checker

import com.intellij.openapi.ui.TestDialog
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.EduProjectSettings

abstract class CheckersTestCommonBase<Settings : EduProjectSettings> : HeavyPlatformTestCase() {
  protected lateinit var myCourse: Course

  private val checkerFixture: EduCheckerFixture<Settings> by lazy {
    createCheckerFixture()
  }

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    val skipTestReason = checkerFixture.getSkipTestReason()
    if (skipTestReason != null) {
      System.err.println("SKIP `$name`: $skipTestReason")
    }
    else {
      super.runTestRunnable(context)
    }
  }

  protected abstract fun createCheckerFixture(): EduCheckerFixture<Settings>
  protected abstract fun createCourse(): Course

  private fun projectName() = getTestName(true)

  override fun setUpProject() {
    checkerFixture.setUp()
    if (checkerFixture.getSkipTestReason() == null) {
      myCourse = createCourse()
      val settings = checkerFixture.projectSettings

      withTestDialog(TestDialog.NO) {
        val rootDir = tempDir.createVirtualDir()
        val generator = myCourse.configurator?.courseBuilder?.getCourseProjectGenerator(myCourse)
                        ?: error("Failed to get `CourseProjectGenerator`")
        myProject = generator.doCreateCourseProject(rootDir.path, settings)
                    ?: error("Cannot create project with name ${projectName()}")
      }
    }
  }

  override fun setUp() {
    super.setUp()

    if (myProject != null) {
      EduDocumentListener.setGlobalListener(myProject, testRootDisposable)
    }

    CheckActionListener.registerListener(testRootDisposable)
    CheckActionListener.reset()
  }

  override fun tearDown() {
    try {
      checkerFixture.tearDown()
    } catch (_: Throwable) {
    } finally {
      super.tearDown()
    }
  }
}
