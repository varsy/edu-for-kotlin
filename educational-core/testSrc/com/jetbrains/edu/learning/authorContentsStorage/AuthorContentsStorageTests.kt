package com.jetbrains.edu.learning.authorContentsStorage

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.apache.commons.io.FileUtils
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists

class AuthorContentsStorageTests : EduTestCase() {

  fun `test author contents is preserved after closing and opening the project`() {
    // create the initial project, this is the project by a course creator
    val authorCourse = courseWithFiles(createYamlConfigs = true) {
      lesson {
        eduTask {
          taskFile("hello.txt", InMemoryTextualContents("hello.txt contents"))
        }
      }
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

    // test that helloTxt contents are preserved
    testContentsIsPreserved(studentProject)

    // close the student project and open it again as "studentProject2"
    ProjectManager.getInstance().closeAndDispose(studentProject)

    val studentProject2 = ProjectManager.getInstance().loadAndOpenProject(studentCourseLocation.toString())
                          ?: error("failed to open project")

    // test again that helloTxt contents are preserved
    testContentsIsPreserved(studentProject2)

    // close and delete all temporary files
    ProjectManager.getInstance().closeAndDispose(studentProject2)
    locationPath.deleteIfExists()
    FileUtils.deleteDirectory(studentCourseLocation.toFile())
  }

  private fun testContentsIsPreserved(studentProject: Project) {
    val helloTxt2 = studentProject.course!!.lessons[0].taskList[0].getTaskFile("hello.txt") ?: error("no hello.txt")
    val helloTxtText2 = helloTxt2.contents as TextualContents
    assertEquals("hello.txt contents", helloTxtText2.text)
  }

}