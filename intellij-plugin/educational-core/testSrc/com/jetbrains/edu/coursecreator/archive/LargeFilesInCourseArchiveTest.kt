package com.jetbrains.edu.coursecreator.archive

import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.learning.EDU_TEST_BIN
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.getBinaryFileLimit
import org.junit.Test

class LargeFilesInCourseArchiveTest : CourseArchiveTestBase() {

  @Test
  fun `test large binary file in a framework lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask {
          taskFile("task.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(getBinaryFileLimit() + 1)))
        }
      }
    }

    createCourseArchiveWithError<HugeBinaryFileError>(course)
  }

  @Test
  fun `test large binary file in a non-framework lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("task.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(FileUtilRt.LARGE_FOR_CONTENT_LOADING + 1)))
        }
      }
    }

    createCourseArchiveWithError<HugeBinaryFileError>(course)
  }

  @Test
  fun `test large binary additional file`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      additionalFile("file.$EDU_TEST_BIN", InMemoryBinaryContents(ByteArray(FileUtilRt.LARGE_FOR_CONTENT_LOADING + 1)))
    }

    createCourseArchiveWithError<HugeBinaryFileError>(course)
  }
}
