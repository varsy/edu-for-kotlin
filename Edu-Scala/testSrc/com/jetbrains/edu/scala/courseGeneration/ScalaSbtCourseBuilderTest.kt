package com.jetbrains.edu.scala.courseGeneration

import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.StudyTaskManager.Companion.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaSbtCourseBuilderTest : JvmCourseGenerationTestBase() {

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/scala_course_sbt.json")
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("TestSpec.scala")
          }
          file("task.html")
          file("build.sbt")
        }
      }
      dir("project") {
        file("build.properties")
      }
      file("build.sbt")
      
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(ScalaLanguage.INSTANCE, environment = "sbt")
    createCourseStructure(course)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("TestSpec.scala")
          }
          file("task.md")
          file("build.sbt")
        }
      }
      dir("project") {
        file("build.properties")
      }
      file("build.sbt")
      
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
