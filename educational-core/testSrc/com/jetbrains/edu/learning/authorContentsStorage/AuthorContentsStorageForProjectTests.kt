package com.jetbrains.edu.learning.authorContentsStorage

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authorContentsStorage.zip.UpdatableZipAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.pathInAuthorContentsStorageForEduFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.apache.commons.io.FileUtils
import java.util.*
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists

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

  fun `test author contents is preserved after a student closes and opens the project`() {
    // create a course archive
    val locationPath = kotlin.io.path.createTempFile("course.json", ".zip").toAbsolutePath()
    CourseArchiveCreator(project, locationPath.toString()).createArchive()

    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    // unpack a course to a new project "studentProject"
    val studentCourseLocation = createTempDirectory("studentCourse")
    val studentCourse = EduUtilsKt.getLocalCourse(locationPath.toString()) ?: error("failed to load local course")

    val studentProject = studentCourse.configurator?.courseBuilder?.getCourseProjectGenerator(studentCourse)?.doCreateCourseProject(
      studentCourseLocation.toString(),
      EmptyProjectSettings
    ) ?: error("failed to create project")

    // create yaml files for a newly created project
    studentProject.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, true)
    YamlFormatSynchronizer.saveAll(studentProject)
    PlatformTestUtil.dispatchAllEventsInIdeEventQueue()

    // test that contents are preserved
    testAllContentsArePreserved(studentProject)

    // close the student project and open it again as "studentProject2"
    ProjectManager.getInstance().closeAndDispose(studentProject)

    val studentProject2 = ProjectManagerEx.getInstanceEx().openProject(
      studentCourseLocation,
      OpenProjectTask(forceOpenInNewFrame = true)
    ) ?: error("failed to open project")

    // test again that contents are preserved
    testAllContentsArePreserved(studentProject2)

    // close and delete all temporary files
    ProjectManager.getInstance().closeAndDispose(studentProject2)
    locationPath.deleteIfExists()
    FileUtils.deleteDirectory(studentCourseLocation.toFile())
  }

  private fun testAllContentsArePreserved(studentProject: Project) {
    val course = studentProject.course!!
    course.visitEduFiles { eduFile ->
      assertFileContentsEqual(expectedContents(eduFile), eduFile.contents)
    }
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