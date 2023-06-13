package com.jetbrains.edu.learning.authorContentsStorage

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authorContentsStorage.zip.UpdatableZipAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.pathInAuthorContentsStorageForEduFile
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import java.util.*

class AuthorContentsStorageForProjectTests : EduTestCase() {

  override fun createCourse() {
    val authorCourse = courseWithFiles(createYamlConfigs = true) {
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
          additionalFile("section1/lesson2/additional_file.txt", InMemoryTextualContents("contents of file section1/lesson2/additional_file.txt"))
          additionalFile("section1/lesson2/additional_file.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
        }
        additionalFile("section1/additional_file.txt", InMemoryTextualContents("contents of file section1/additional_file.txt"))
        additionalFile("section1/additional_file.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
      }
      additionalFile("additional_file.txt", InMemoryTextualContents("contents of file additional_file.txt"))
      additionalFile("additional_file.png", InMemoryBinaryContents(byteArrayOf(10, 20, 30)))
    }

    StudyTaskManager.getInstance(project).course = authorCourse
  }

  fun `test updatable author contents storage`() {
    val storage = UpdatableZipAuthorContentsStorage(project.courseDir)
    val course = project.course ?: throw IllegalStateException("failed to get course")
    storage.update(course)

    course.visitEduFiles { eduFile ->
      val path = pathInAuthorContentsStorageForEduFile(eduFile)
      assertFileContentsEqual(expectedContents(eduFile), storage.get(path))
    }

    // update task1 and check that contents in storage are replaced
    val task1 = course.findTask("lesson1", "task1")
    task1.taskFiles["hello.txt"]?.contents = InMemoryTextualContents("changed text")
    task1.taskFiles["hello.png"]?.contents = InMemoryBinaryContents(byteArrayOf(40, 50, 60))

    storage.update(course)

    assertFileContentsEqual(InMemoryTextualContents("changed text"), storage.get("lesson1/task1/hello.txt"))
    assertFileContentsEqual(InMemoryBinaryContents(byteArrayOf(40, 50, 60)), storage.get("lesson1/task1/hello.png"))

    // update task2 with undefined contents and check that contents in storage are replaced
    val task2 = course.findTask("lesson1", "task2")
    task2.taskFiles["hello.txt"]?.contents = InMemoryUndeterminedContents("changed text")
    task2.taskFiles["hello.png"]?.contents = InMemoryUndeterminedContents(
      Base64.getEncoder().encodeToString(byteArrayOf(40, 50, 60))
    )

    storage.update(course)

    assertFileContentsEqual(InMemoryTextualContents("changed text"), storage.get("lesson1/task2/hello.txt"))
    assertFileContentsEqual(InMemoryBinaryContents(byteArrayOf(40, 50, 60)), storage.get("lesson1/task2/hello.png"))
  }

  private fun expectedContents(eduFile: EduFile): FileContents {
    val path = pathInAuthorContentsStorageForEduFile(eduFile)
    return if (path.endsWith(".png")) {
      InMemoryBinaryContents(byteArrayOf(10, 20, 30))
    }
    else {
      InMemoryTextualContents("contents of file $path")
    }
  }
}