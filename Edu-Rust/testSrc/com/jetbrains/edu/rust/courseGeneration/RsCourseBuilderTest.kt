package com.jetbrains.edu.rust.courseGeneration

import com.jetbrains.edu.learning.authorContentsStorage.zip.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.lang.RsLanguage

class RsCourseBuilderTest : CourseGenerationTestBase<RsProjectSettings>() {

  override val defaultSettings: RsProjectSettings = RsProjectSettings(null)

  fun `test new educator course`() {
    val newCourse = newCourse(RsLanguage)
    createCourseStructure(newCourse)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("lib.rs")
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("Cargo.toml")
        file("task.md")
      }
      file("Cargo.toml", """
          [workspace]
          
          members = [
              "lesson1/*/",
          ]
          
          exclude = [
              "**/*.yaml"
          ]

      """)
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }.assertEquals(rootDir)
  }

  fun `test create existent educator course`() {
    val course = course(language = RsLanguage, courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("src/main.rs")
          taskFile("tests/tests.rs")
          taskFile("Cargo.toml")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("Cargo.toml")
        file("task.md")
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }.assertEquals(rootDir)
  }


  fun `test study course structure`() {
    val course = course(language = RsLanguage) {
      lesson {
        eduTask {
          taskFile("src/main.rs")
          taskFile("tests/tests.rs")
          taskFile("Cargo.toml")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("task.md")
        file("Cargo.toml")
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }.assertEquals(rootDir)
  }
}
