package com.jetbrains.edu.sql.java.courseGeneration

import com.intellij.sql.psi.SqlLanguage
import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.authorContentsStorage.zip.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse

class SqlJCourseBuilderTest : JvmCourseGenerationTestBase() {

  fun `test new educator course`() {
    val newCourse = newCourse(SqlLanguage.INSTANCE, environment = "Java")
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
        }
        dir("test") {
          file("Tests.java")
        }
        file("init.sql")
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  fun `test create existent educator course`() {
    val course = course(language = SqlLanguage.INSTANCE, courseMode = CourseMode.EDUCATOR, environment = "Java") {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/Tests.java")
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
          file("Tests.java")
        }
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  fun `test study course structure`() {
    val course = course(language = SqlLanguage.INSTANCE, environment = "Java") {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/Tests.java")
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
          file("Tests.java")
        }
        file("task.md")
      }
      file("build.gradle")
      file("settings.gradle")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }.assertEquals(rootDir)
  }
}
