package com.jetbrains.edu.learning.binaryTextualTaskFiles

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseReopeningTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings

class TestBinaryFilesAreNotStoredInsideTasks : CourseReopeningTestBase<EmptyProjectSettings>() {

  override val defaultSettings = EmptyProjectSettings

  /**
   * This test describes the current behaviour of the Plugin. Actually, we want all the contents to be preserved after reopening the course
   * So this test will be rewritten in the future.
   */
  fun `test binary files are not preserved after a student closes and opens the project`() {
    /**
     * We create files here in such a way that their contents depend on their path.
     * So, file contents are different, but we can later check file contents knowing only their path.
     */
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("hello.txt", InMemoryTextualContents("contents of file lesson1/task1/hello.txt"))
          taskFile("hello.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
        }
        eduTask("task2") {
          taskFile("hello.txt", InMemoryTextualContents("contents of file lesson1/task2/hello.txt"))
          taskFile("hello.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
        }
      }
      section("section1") {
        lesson("lesson2") {
          eduTask("task1") {
            taskFile("hello.txt", InMemoryTextualContents("contents of file section1/lesson2/task1/hello.txt"))
            taskFile("hello.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
          }
        }
      }
      additionalFile("section1/lesson2/additional_file.txt", InMemoryTextualContents("contents of file section1/lesson2/additional_file.txt"))
      additionalFile("section1/lesson2/additional_file.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
      additionalFile("section1/additional_file.txt", InMemoryTextualContents("contents of file section1/additional_file.txt"))
      additionalFile("section1/additional_file.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
      additionalFile("additional_file.txt", InMemoryTextualContents("contents of file additional_file.txt"))
      additionalFile("additional_file.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
    }

    openStudentProjectThenReopenStudentProject(course, {
      testContentsArePreserved(it)
    }, {
      testContentsArePreserved(it, binaryFilesCleared = true)
    })
  }

  private fun testContentsArePreserved(studentProject: Project, binaryFilesCleared: Boolean = false) {
    val course = studentProject.course!!
    course.visitEduFiles { eduFile ->
      val path = eduFile.pathInCourse()
      var expectedContents = expectedContents(eduFile)
      if (binaryFilesCleared && expectedContents is BinaryContents)
        expectedContents = UndeterminedContents.EMPTY
      assertContentsEqual(path, expectedContents, eduFile.contents)
    }
  }

  private fun EduFile.pathInCourse(): String {
    val pathPrefix = if (this is TaskFile) task.getPathInCourse() else ""
    val pathInCourse = "$pathPrefix/$name"

    return if (pathInCourse.startsWith('/')) {
      pathInCourse.substring(1)
    }
    else {
      pathInCourse
    }
  }

  private fun expectedContents(eduFile: EduFile): FileContents {
    val path = eduFile.pathInCourse()

    return if (path.endsWith(".png")) {
      InMemoryBinaryContents(byteArrayOf(10, 20, 30))
    }
    else {
      InMemoryTextualContents("contents of file $path")
    }
  }
}