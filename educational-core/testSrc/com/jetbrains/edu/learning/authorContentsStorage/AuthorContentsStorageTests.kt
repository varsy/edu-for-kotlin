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
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.pathInAuthorContentsStorageForEduFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.apache.commons.io.FileUtils
import org.junit.Assert.assertArrayEquals
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists

class AuthorContentsStorageTests : EduTestCase() {

  fun `test author contents is preserved after a student closes and opens the project`() {
    // create the initial project, this is the project by a course creator
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

    // create a course archive
    val locationPath = createTempFile("course.json", ".zip").toAbsolutePath()
    StudyTaskManager.getInstance(project).course = authorCourse
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
    course.visitTasks { task ->
       for (taskFile in task.taskFiles.values) {
         testEduFileContentsArePreserved(taskFile)
       }
    }

    for (additionalFile in course.additionalFiles)
      testEduFileContentsArePreserved(additionalFile)
  }

  private fun testEduFileContentsArePreserved(eduFile: EduFile) {
    val path = pathInAuthorContentsStorageForEduFile(eduFile)
    val contents = eduFile.contents
    if (contents.isBinary == true) {
      assertArrayEquals(byteArrayOf(10, 20, 30), (contents as BinaryContents).bytes)
    }
    else {
      assertEquals("contents of file $path", (contents as TextualContents).text)
    }
  }

}