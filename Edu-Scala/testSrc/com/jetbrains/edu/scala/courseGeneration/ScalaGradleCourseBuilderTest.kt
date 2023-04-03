package com.jetbrains.edu.scala.courseGeneration

import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.authorContentsStorage.zip.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaGradleCourseBuilderTest : JvmCourseGenerationTestBase() {

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/scala_course.json")
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("my task 1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("Test.scala")
          }
          file("task.html")
        }
        dir("my task 2") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("Test.scala")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(ScalaLanguage.INSTANCE, environment = "Gradle")
    createCourseStructure(course)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("Test.scala")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
