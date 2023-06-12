package com.jetbrains.edu.learning.authorContentsStorage

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.fileContents.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.fileContents.FileContents
import com.jetbrains.edu.learning.courseFormat.fileContents.TextualContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.StorageAuthorContentsHolder
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.apache.commons.io.FileUtils
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists

class AuthorContentsStorageForProjectTests : EduTestCase() {

  override fun createCourse() {
    val authorCourse = courseWithFiles(createYamlConfigs = true) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("hello.txt", TextualContents("contents of file lesson1/task1/hello.txt"))
          taskFile("hello.png", BinaryContents(byteArrayOf(10, 20, 30)))
        }
        eduTask("task2") {
          taskFile("hello.txt", TextualContents("contents of file lesson1/task2/hello.txt"))
          taskFile("hello.png", BinaryContents(byteArrayOf(10, 20, 30)))
        }
      }
      section("section1") {
        lesson("lesson2") {
          eduTask("task1") {
            taskFile("hello.txt", TextualContents("contents of file section1/lesson2/task1/hello.txt"))
            taskFile("hello.png", BinaryContents(byteArrayOf(10, 20, 30)))
          }
          additionalFile("section1/lesson2/additional_file.txt", TextualContents("contents of file section1/lesson2/additional_file.txt"))
          additionalFile("section1/lesson2/additional_file.png", BinaryContents(byteArrayOf(10, 20, 30)))
        }
        additionalFile("section1/additional_file.txt", TextualContents("contents of file section1/additional_file.txt"))
        additionalFile("section1/additional_file.png", BinaryContents(byteArrayOf(10, 20, 30)))
      }
      additionalFile("additional_file.txt", TextualContents("contents of file additional_file.txt"))
      additionalFile("additional_file.png", BinaryContents(byteArrayOf(10, 20, 30)))
    }

    StudyTaskManager.getInstance(project).course = authorCourse
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
      assertEquals(expectedContents(eduFile), eduFile.contents)
    }
  }

  private fun expectedContents(eduFile: EduFile): FileContents {
    val path = object : StorageAuthorContentsHolder(eduFile) {
      override val storage: AuthorContentsStorage? = null
    }.path

    return if (path.endsWith(".png")) {
      BinaryContents(byteArrayOf(10, 20, 30))
    }
    else {
      TextualContents("contents of file $path")
    }
  }
}