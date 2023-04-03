package com.jetbrains.edu.kotlin.courseGeneration

import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.authorContentsStorage.zip.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtCourseBuilderTest : JvmCourseGenerationTestBase() {

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/kotlin_course.json")
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Hello, world") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
        dir("Java to Kotlin conversion") {
          dir("src") {
            file("JavaCode.java")
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
      }
      dir("Conventions") {
        dir("Comparison") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
      }
      dir("util") {
        dir("src") {
          file("koansTestUtil.kt")
        }
      }
      file("build.gradle")
      file("settings.gradle")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }
    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(KotlinLanguage.INSTANCE)
    createCourseStructure(course)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.md")
        }
      }
      file("build.gradle")
      file("settings.gradle")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test educator course structure from not empty course`() {
    generateCourseStructure("testData/newCourse/kotlin_course.json", CourseMode.EDUCATOR)
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Hello, world") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
        dir("Java to Kotlin conversion") {
          dir("src") {
            file("JavaCode.java")
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
      }
      dir("Conventions") {
        dir("Comparison") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("tests.kt")
          }
          file("task.html")
        }
      }
      dir("util") {
        dir("src") {
          file("koansTestUtil.kt")
        }
      }
      file("build.gradle")
      file("settings.gradle")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }
    expectedFileTree.assertEquals(rootDir)
  }
}
