package com.jetbrains.edu.python.courseGeneration

import com.jetbrains.edu.learning.StudyTaskManager.Companion.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newCourse
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import com.jetbrains.python.PythonLanguage

class PyCourseBuilderTest : CourseGenerationTestBase<PyProjectSettings>() {

  override val defaultSettings: PyProjectSettings = PyProjectSettings()

  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/python_course.json")
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Our first program") {
          file("hello_world.py")
          file("tests.py")
          file("task.html")
        }
        dir("Comments") {
          file("comments.py")
          file("tests.py")
          file("task.html")
        }
      }
      dir("Variables") {
        dir("Variable definition") {
          file("variable_definition.py")
          file("tests.py")
          file("task.html")
        }
      }
      file("test_helper.py")
      
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test new course structure`() {
    val course = newCourse(PythonLanguage.INSTANCE)
    createCourseStructure(course)

    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("task.py")
          file("tests.py")
          file("task.md")
        }
      }
      
    }

    expectedFileTree.assertEquals(rootDir)
  }

  fun `test educator course structure from not empty course`() {
    generateCourseStructure("testData/newCourse/python_course.json", CourseMode.EDUCATOR)
    val expectedFileTree = fileTree {
      dir("Introduction") {
        dir("Our first program") {
          file("hello_world.py")
          file("tests.py")
          file("task.html")
        }
        dir("Comments") {
          file("comments.py")
          file("tests.py")
          file("task.html")
        }
      }
      dir("Variables") {
        dir("Variable definition") {
          file("variable_definition.py")
          file("tests.py")
          file("task.html")
        }
      }
      file("test_helper.py")
      
    }

    expectedFileTree.assertEquals(rootDir)
  }
}
