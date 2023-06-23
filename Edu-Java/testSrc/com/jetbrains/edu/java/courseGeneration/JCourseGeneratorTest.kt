package com.jetbrains.edu.java.courseGeneration

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.StudyTaskManager.Companion.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse

class JCourseGeneratorTest : JvmCourseGenerationTestBase() {

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/java_course.json")
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.html")
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
      
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(JavaLanguage.INSTANCE)
    createCourseStructure(course)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
      
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test educator course structure from not empty course`() {
    generateCourseStructure("testData/newCourse/java_course.json", CourseMode.EDUCATOR)
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.html")
        }
      }
      dir("lesson2") {
        dir("task1") {
          dir("src") {
            file("Task.java")
          }
          dir("test") {
            file("Tests.java")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
      
    }
    expectedFileTree.assertEquals(rootDir)
  }
}
