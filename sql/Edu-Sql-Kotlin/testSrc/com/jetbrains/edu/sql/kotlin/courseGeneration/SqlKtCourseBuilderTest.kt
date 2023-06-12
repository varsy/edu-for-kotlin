package com.jetbrains.edu.sql.kotlin.courseGeneration

import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse

class SqlKtCourseBuilderTest : JvmCourseGenerationTestBase() {

  fun `test new educator course`() {
    val newCourse = newCourse(SqlLanguage.INSTANCE, environment = "Kotlin")
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
        }
        dir("test") {
          file("Tests.kt")
        }
        file("init.sql")
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  fun `test create existent educator course`() {
    val course = course(language = SqlLanguage.INSTANCE, courseMode = CourseMode.EDUCATOR, environment = "Kotlin") {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/Tests.kt")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
          file("migration.sql")
          dir("data") {
            file("data.sql")
          }
        }
        dir("test") {
          file("Tests.kt")
        }
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  fun `test study course structure`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Kotlin") {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/Tests.kt")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
          file("migration.sql")
          dir("data") {
            file("data.sql")
          }
        }
        dir("test") {
          file("Tests.kt")
        }
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }
}
