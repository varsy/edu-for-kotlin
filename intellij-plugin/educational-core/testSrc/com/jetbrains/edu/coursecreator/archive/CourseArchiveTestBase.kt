package com.jetbrains.edu.coursecreator.archive

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.cipher.Cipher
import com.jetbrains.edu.learning.cipher.NoOpCipher
import com.jetbrains.edu.learning.courseFormat.Course
import java.io.File
import kotlin.test.assertIs

abstract class CourseArchiveTestBase : EduActionTestCase() {

  /**
   * Launches course archive creation for given [course] and checks:
   * - course archive creation was successful
   * - created course archive contains the same files as `[getTestDataPath]/<testName>` directory
   *
   * If [generateExtraFiles], creates files contained in actual course archive but not present in test data.
   * It's supposed to be used to simplify new test creation
   */
  protected fun doTest(
    course: Course,
    cipher: Cipher = NoOpCipher(),
    generateExtraFiles: Boolean = false
  ) {
    val courseArchiveContent = createCourseArchiveAndCheck(course, cipher)
    val expectedCourseArchiveContent = collectExpectedCourseArchiveContent()

    courseArchiveContent.assertEquals(expectedCourseArchiveContent, getExpectedDataDirectory(), generateExtraFiles)
  }

  protected fun createCourseArchive(course: Course, cipher: Cipher = NoOpCipher()): Result<CourseArchiveContent, CourseArchiveError> {
    val outputProducer = TestCourseArchiveOutputProducer()

    val creator = CourseArchiveCreator(myFixture.project, outputProducer, cipher)
    val errorMessage = creator.createArchive(course)

    return if (errorMessage != null) {
      Err(errorMessage)
    }
    else {
      Ok(CourseArchiveContent.fromBytes(outputProducer.data()))
    }
  }

  protected fun createCourseArchiveAndCheck(course: Course, cipher: Cipher = NoOpCipher()): CourseArchiveContent {
    return createCourseArchive(course, cipher).onError { error ->
      kotlin.test.fail("Course creation failed with error: (${error.javaClass}) ${error.message}")
    }
  }

  protected inline fun <reified T : CourseArchiveError> createCourseArchiveWithError(course: Course): T {
    val result = createCourseArchive(course)
    assertIs<Err<*>>(result, "Course creation must generate an error message")
    return assertIs<T>(result.error, "Error must be of type ${T::class.simpleName}")
  }

  private fun collectExpectedCourseArchiveContent(): CourseArchiveContent {
    val expectedDataDir = getExpectedDataDirectory()

    val files = mutableMapOf<String, ByteArray>()
    for (file in expectedDataDir.walkTopDown()) {
      if (file.isDirectory) continue

      val relativePath = file.relativeTo(expectedDataDir).invariantSeparatorsPath
      files[relativePath] = file.readBytes()
    }

    return CourseArchiveContent(files)
  }

  private fun getExpectedDataDirectory(): File {
    return File(testDataPath, getTestName(true).trim())
  }
}
