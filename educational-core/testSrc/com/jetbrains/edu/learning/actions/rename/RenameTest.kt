package com.jetbrains.edu.learning.actions.rename

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.testAction

class RenameTest : RenameTestBase() {

  fun `test forbid section renaming in student mode`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }

    doRenameAction(course, "section1", "section2", shouldBeInvoked = false)
    assertEquals(1, course.items.size)
    assertNull(course.getSection("section2"))
    assertNotNull(course.getSection("section1"))
  }

  fun `test forbid lesson renaming in section in student mode`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }

    doRenameAction(course, "section1/lesson1", "lesson2", shouldBeInvoked = false)
    assertEquals(1, course.items.size)
    val section = course.getSection("section1")!!
    assertNotNull(section)
    assertNotNull(section.getLesson("lesson1"))
    assertNull(section.getLesson("lesson2"))
  }

  fun `test forbid lesson renaming in course in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    doRenameAction(course, "lesson1", "lesson2", shouldBeInvoked = false)
    assertEquals(1, course.items.size)
    assertNotNull(course.getLesson("lesson1"))
    assertNull(course.getLesson("lesson2"))
  }

  fun `test forbid task renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    doRenameAction(course, "lesson1/task1", "task2", shouldBeInvoked = false)
    assertEquals(1, course.items.size)
    val lesson = course.getLesson("lesson1")!!
    assertNotNull(lesson)
    assertNotNull(lesson.getTask("task1"))
    assertNull(lesson.getTask("task2"))
  }

  fun `test forbid task description file renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    doRenameAction(course, "lesson1/task1/${TASK_MD}", TASK_HTML, shouldBeInvoked = false)
    assertEquals(DescriptionFormat.MD, course.lessons[0].taskList[0].descriptionFormat)
    assertNull(findDescriptionFile(TASK_HTML))
    assertNotNull(findDescriptionFile(TASK_MD))
  }

  fun `test forbid task file renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    doRenameAction(course, "lesson1/task1/taskFile1.txt", "taskFile2.txt", shouldBeInvoked = false)
    val task = course.findTask("lesson1", "task1")
    assertNull(task.getTaskFile("taskFile2.txt"))
    assertNotNull(task.getTaskFile("taskFile1.txt"))
  }

  fun `test rename student created task file in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }

    }
    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, findFile("lesson1/task1"), "taskFile2.txt", "")
    }
    doRenameAction(course, "lesson1/task1/taskFile2.txt", "taskFile3.txt", shouldBeShown = false)
    val task = course.findTask("lesson1", "task1")
    assertNull(task.getTaskFile("taskFile2.txt"))
    assertNotNull(task.getTaskFile("taskFile3.txt"))
  }

  fun `test forbid course additional file renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      additionalFile("additionalFile1.txt")
    }
    doRenameAction(course, "additionalFile1.txt", "additionalFile2.txt", shouldBeInvoked = false)
    assertNull(LightPlatformTestCase.getSourceRoot().findFileByRelativePath("additionalFile2.txt"))
    assertNull(course.additionalFiles.find { it.name == "additionalFile2.txt" })
    assertNotNull(LightPlatformTestCase.getSourceRoot().findFileByRelativePath("additionalFile1.txt"))
    assertNotNull(course.additionalFiles.find { it.name == "additionalFile1.txt" })
  }

  fun `test rename student created task file in student mode in hyperskill course`() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask("task1") {
          taskFile("taskFile1.txt")
        }
        eduTask("task2") {
          taskFile("taskFile1.txt")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2, true))

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, findFile("lesson1/task"), "taskFile2.txt", "")
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("taskFile2.txt")
      testAction(NextTaskAction.ACTION_ID)
    }

    doRenameAction(course, "lesson1/task/taskFile2.txt", "taskFile3.txt", shouldBeShown = false)

    val task2 = course.findTask("lesson1", "task2")
    assertNull(task2.getTaskFile("taskFile2.txt"))
    assertNotNull(task2.getTaskFile("taskFile3.txt"))
  }

  fun `test rename section in CC mode`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }

    doRenameAction(course, "section1", "section2")
    assertEquals(1, course.items.size)
    assertNull(course.getSection("section1"))
    assertNotNull(course.getSection("section2"))
  }

  fun `test rename lesson in section in CC mode`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }

    doRenameAction(course, "section1/lesson1", "lesson2")
    assertEquals(1, course.items.size)
    val section = course.getSection("section1")!!
    assertNotNull(section)
    assertNotNull(section.getLesson("lesson2"))
    assertNull(section.getLesson("lesson1"))
  }

  fun `test rename lesson in course in CC mode`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    doRenameAction(course, "lesson1", "lesson2")
    assertEquals(1, course.items.size)
    assertNotNull(course.getLesson("lesson2"))
    assertNull(course.getLesson("lesson1"))
  }

  fun `test rename task in CC mode`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    doRenameAction(course, "lesson1/task1", "task2")
    assertEquals(1, course.items.size)
    val lesson = course.getLesson("lesson1")!!
    assertNotNull(lesson)
    assertNotNull(lesson.getTask("task2"))
    assertNull(lesson.getTask("task1"))
  }

  fun `test rename task description file in CC mode`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    doRenameAction(course, "lesson1/task1/${TASK_MD}", TASK_HTML)
    assertEquals(DescriptionFormat.HTML, course.lessons[0].taskList[0].descriptionFormat)
    assertNull(findDescriptionFile(TASK_MD))
    assertNotNull(findDescriptionFile(TASK_HTML))
  }

  fun `test wrong new task description file name in CC mode`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    val newFileName = "incorrectFileName.txt"
    doRenameAction(course, "lesson1/task1/${TASK_MD}", newFileName, shouldBeInvoked = false)
    assertEquals(DescriptionFormat.MD, course.lessons[0].taskList[0].descriptionFormat)
    assertNull(findDescriptionFile(newFileName))
    assertNotNull(findDescriptionFile(TASK_MD))
  }

  fun `test rename task file in CC mode`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    // When there isn't any rename handler for file, rename action uses default one.
    // And default rename handler has special code for unit tests not to show rename dialog at all
    doRenameAction(course, "lesson1/task1/taskFile1.txt", "taskFile2.txt", shouldBeShown = false)
    val task = course.findTask("lesson1", "task1")
    assertNull(task.getTaskFile("taskFile1.txt"))
    assertNotNull(task.getTaskFile("taskFile2.txt"))
  }
}
