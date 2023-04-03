package com.jetbrains.edu.cpp.courseGeneration

import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.cpp.getExpectedTaskCMakeText
import com.jetbrains.edu.learning.authorContentsStorage.zip.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree

class CppCourseBuilderTest : CourseGenerationTestBase<CppProjectSettings>() {

  override val defaultSettings = CppProjectSettings()

  fun `test create new cc edu GoogleTest course`() {
    val course = course(
      language = OCLanguage.getInstance(),
      courseMode = CourseMode.EDUCATOR,
      environment = "GoogleTest"
    ) { }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.md")
        file("CMakeLists.txt",
             getExpectedTaskCMakeText(course, defaultSettings, "global-lesson1-task1"))
      }
      dir("cmake") {
        file("utils.cmake")
        file("googletest.cmake")
        file("googletest-download.cmake")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)

  }

  fun `test create new cc edu Catch course`() {
    val course = course(
      language = OCLanguage.getInstance(),
      courseMode = CourseMode.EDUCATOR,
      environment = "Catch"
    ) {}
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.md")
        file("CMakeLists.txt",
             getExpectedTaskCMakeText(course, defaultSettings, "global-lesson1-task1"))
      }
      dir("cmake") {
        file("utils.cmake")
        file("catch.cmake")
      }
      file("CMakeLists.txt")
    }.assertEquals(rootDir)

  }

  fun `test study edu course structure with top-level lesson`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      lesson("lesson") {
        eduTask("task") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
      }
      additionalFiles {
        eduFile("CMakeLists.txt.in")
        eduFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.md")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }.assertEquals(rootDir)

  }

  fun `test study edu course structure with section`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      section("section") {
        lesson("lesson") {
          eduTask("task") {
            taskFile("src/task.cpp")
            taskFile("test/test.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }
      additionalFiles {
        eduFile("CMakeLists.txt.in")
        eduFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section/lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.md")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }.assertEquals(rootDir)

  }

  fun `test study course structure with top-level section and lesson`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      section("section") {
        lesson("lesson") {
          eduTask("task") {
            taskFile("src/task.cpp")
            taskFile("test/test.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }
      lesson("top_level_lesson") {
        eduTask("task") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
      }
      additionalFiles {
        eduFile("CMakeLists.txt.in")
        eduFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section/lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.md")
        file("CMakeLists.txt")
      }
      dir("top_level_lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.md")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }.assertEquals(rootDir)

  }

  fun `test study edu course structure with different tasks`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      lesson("lesson") {
        eduTask("edu") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
        outputTask("output") {
          taskFile("src/task.cpp")
          taskFile("output.txt")
          taskFile("CMakeLists.txt")
        }
        theoryTask("theory") {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }
      }

      additionalFiles {
        eduFile("CMakeLists.txt.in")
        eduFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson") {
        dir("edu") {
          dir("src") {
            file("task.cpp")
          }
          dir("test") {
            file("test.cpp")
          }
          file("task.md")
          file("CMakeLists.txt")
        }
        dir("output") {
          dir("src") {
            file("task.cpp")
          }
          file("output.txt")
          file("task.md")
          file("CMakeLists.txt")
        }
        dir("theory") {
          dir("src") {
            file("task.cpp")
          }
          file("task.md")
          file("CMakeLists.txt")
        }
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }.assertEquals(rootDir)
  }
}
