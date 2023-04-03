package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.EduUtils.getFirstTask
import com.jetbrains.edu.learning.FileTree
import com.jetbrains.edu.learning.authorContentsStorage.zip.COURSE_AUTHOR_CONTENTS_FILE
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdater
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.rd.util.firstOrNull
import java.util.*

class MarketplaceCourseUpdateTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  fun `test update date updated`() {
    val course = createCourse(CheckStatus.Solved)
    course.updateDate = Date(1619697473000)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2Renamed.kt")
        }
      }
    }.apply { updateDate = Date(1624354026000) } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2Renamed.kt")
          file("task.md")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(course.updateDate, serverCourse.updateDate)
  }

  fun `test save task status Solved if task not updated`() {
    val course = createCourse(CheckStatus.Solved)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2Renamed.kt")
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2Renamed.kt")
          file("task.md")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Solved, getFirstTask(course)?.status)
  }

  fun `test save task status Failed if task not updated`() {
    val course = createCourse(CheckStatus.Failed)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2Renamed.kt")
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2Renamed.kt")
          file("task.md")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Failed, getFirstTask(course)?.status)
  }

  fun `test save task status Solved if task was updated`() {
    val course = createCourse(CheckStatus.Solved)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1Renamed.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2.kt")
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1Renamed.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2.kt")
          file("task.md")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Solved, getFirstTask(course)?.status)
  }

  fun `test do not save task status Failed if task was updated`() {
    val course = createCourse(CheckStatus.Failed)

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1Renamed.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2.kt")
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1Renamed.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2.kt")
          file("task.md")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, serverCourse, expectedStructure, 2)
    assertEquals(CheckStatus.Unchecked, getFirstTask(course)?.status)
  }

  fun `test placeholder possible answer changed`() {
    val course = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "\"Bar\"")
          }
        }
      }
    } as EduCourse
    course.marketplaceCourseVersion = 1

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "\"Updated\"")
          }
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("Buzz.kt")
          file("task.md")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, serverCourse, expectedStructure, 2)
    val placeholder = getFirstTask(course)?.taskFiles?.firstOrNull()?.value?.answerPlaceholders?.firstOrNull()
    checkNotNull(placeholder)
    assertEquals("\"Updated\"", placeholder.possibleAnswer)
  }

  fun `test placeholder length changed`() {
    val course = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "\"Bar\"")
          }
        }
      }
    } as EduCourse
    course.marketplaceCourseVersion = 1

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO</p>()") {
            placeholder(0, "\"Updated\"")
          }
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("Buzz.kt")
          file("task.md")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, serverCourse, expectedStructure, 2)
    val placeholder = getFirstTask(course)?.taskFiles?.firstOrNull()?.value?.answerPlaceholders?.firstOrNull()
    checkNotNull(placeholder)
    assertEquals(4, placeholder.length)
  }

  fun `test placeholder dependency changed`() {
    val course = course {
      lesson {
        eduTask(stepId = 1, name = "task1") {
          taskFile("TaskFile1.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }
        eduTask(stepId = 2, name = "task2") {
          taskFile("TaskFile2.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }
        eduTask(stepId = 3, name = "task3") {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "123", dependency = "lesson1#task1#TaskFile1.kt#1")
          }
        }
      }
    } as EduCourse
    course.marketplaceCourseVersion = 1

    val serverCourse = course {
      lesson {
        eduTask(stepId = 1, name = "task1") {
          taskFile("TaskFile1.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }
        eduTask(stepId = 2, name = "task2") {
          taskFile("TaskFile2.kt", "fn foo() = <p>TODO()</p>") {
            placeholder(0, "123")
          }
        }
        eduTask(stepId = 3, name = "task3") {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "123", dependency = "lesson1#task2#TaskFile2.kt#1")
          }
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("TaskFile1.kt")
          file("task.md")
        }
        dir("task2") {
          file("TaskFile2.kt")
          file("task.md")
        }
        dir("task3") {
          file("Buzz.kt")
          file("task.md")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }
    doTest(course, serverCourse, expectedStructure, 2)
    val placeholder = course.allTasks[2].taskFiles.firstOrNull()?.value?.answerPlaceholders?.firstOrNull()
    checkNotNull(placeholder)
    assertEquals("lesson1#task2#TaskFile2.kt#1", placeholder.placeholderDependency.toString())
  }

  fun `test framework lesson in section first task updated`() {
    val taskFileName = "src/Task.kt"
    val testFileName = "test/Tests.kt"
    val oldTaskFileText = "fun foo() {}"
    val oldTestFileText = "fun test() {}"
    val oldTaskDescriptionText = "Old Description"
    val updatedTaskFileText = "fun updated() {}"
    val updatedTestFileText = "fun updatedTest() {}"
    val updatedTaskDescriptionText = "New Description"

    val course = course {
      section("section1") {
        frameworkLesson("lesson1") {
          eduTask("task1", stepId = 1, taskDescription = oldTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile(taskFileName, oldTaskFileText)
            taskFile(testFileName, oldTestFileText)
          }
          eduTask("task2", stepId = 2, taskDescription = oldTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile(taskFileName, oldTaskFileText)
            taskFile(testFileName, oldTestFileText)
          }
        }
      }
    } as EduCourse
    course.marketplaceCourseVersion = 1

    val courseFromServer = course {
      section("section1") {
        frameworkLesson("lesson1") {
          eduTask("task1", stepId = 1, taskDescription = updatedTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile(taskFileName, updatedTaskFileText)
            taskFile(testFileName, updatedTestFileText)

          }
          eduTask("task2", stepId = 2, taskDescription = oldTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile(taskFileName, oldTaskFileText)
            taskFile(testFileName, oldTestFileText)
          }
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt")
            }
            dir("test") {
              file("Tests.kt")
            }
          }
          dir("task1") {
            file("task.html")
          }
          dir("task2") {
            file("task.html")
          }
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, courseFromServer, expectedStructure, 2)

    val firstTask = getFirstTask(course)
    checkNotNull(firstTask)
    checkTaskFiles(firstTask, updatedTaskFileText, updatedTestFileText, updatedTaskDescriptionText, taskFileName, testFileName)

    val allTasks = course.allTasks
    assertEquals(2, allTasks.size)
    val secondTask = allTasks[1]
    checkTaskFiles(secondTask, oldTaskFileText, oldTestFileText, oldTaskDescriptionText, taskFileName, testFileName)
  }

  fun `test framework lesson not updated if tasks number on remote decreased`() {
    val taskFileName = "src/Task.kt"
    val testFileName = "test/Tests.kt"
    val oldTaskFileText = "fun foo() {}"
    val oldTestFileText = "fun test() {}"
    val oldTaskDescriptionText = "Old Description"
    val updatedTaskFileText = "fun updated() {}"
    val updatedTestFileText = "fun updatedTest() {}"
    val updatedTaskDescriptionText = "New Description"

    val course = createCourseWithFrameworkLesson(
      taskFileName,
      testFileName,
      oldTaskFileText,
      oldTestFileText,
      oldTaskDescriptionText
    )
    course.marketplaceCourseVersion = 1

    val courseFromServer = course {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1, taskDescription = updatedTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile(taskFileName, updatedTaskFileText)
          taskFile(testFileName, updatedTestFileText)
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, courseFromServer, expectedStructure, 2)

    val firstTask = getFirstTask(course)
    checkNotNull(firstTask)
    checkTaskFiles(firstTask, oldTaskFileText, oldTestFileText, oldTaskDescriptionText, taskFileName, testFileName)

    val allTasks = course.allTasks
    assertEquals(2, allTasks.size)
    val secondTask = allTasks[1]
    checkTaskFiles(secondTask, oldTaskFileText, oldTestFileText, oldTaskDescriptionText, taskFileName, testFileName)
  }

  fun `test framework lesson not updated if tasks number on remote increased`() {
    val taskFileName = "src/Task.kt"
    val testFileName = "test/Tests.kt"
    val oldTaskFileText = "fun foo() {}"
    val oldTestFileText = "fun test() {}"
    val oldTaskDescriptionText = "Old Description"
    val updatedTaskFileText = "fun updated() {}"
    val updatedTestFileText = "fun updatedTest() {}"
    val updatedTaskDescriptionText = "New Description"

    val course = createCourseWithFrameworkLesson(
      taskFileName,
      testFileName,
      oldTaskFileText,
      oldTestFileText,
      oldTaskDescriptionText
    )
    course.marketplaceCourseVersion = 1

    val courseFromServer = course {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1, taskDescription = updatedTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile(taskFileName, updatedTaskFileText)
          taskFile(testFileName, updatedTestFileText)
        }
        eduTask("task1", stepId = 2, taskDescription = updatedTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile(taskFileName, updatedTaskFileText)
          taskFile(testFileName, updatedTestFileText)
        }
        eduTask("task1", stepId = 3, taskDescription = updatedTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile(taskFileName, updatedTaskFileText)
          taskFile(testFileName, updatedTestFileText)
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, courseFromServer, expectedStructure, 2)

    val firstTask = getFirstTask(course)
    checkNotNull(firstTask)
    checkTaskFiles(firstTask, oldTaskFileText, oldTestFileText, oldTaskDescriptionText, taskFileName, testFileName)

    val allTasks = course.allTasks
    assertEquals(2, allTasks.size)
    val secondTask = allTasks[1]
    checkTaskFiles(secondTask, oldTaskFileText, oldTestFileText, oldTaskDescriptionText, taskFileName, testFileName)
  }

  fun `test framework lesson not updated if tasks ids changed`() {
    val taskFileName = "src/Task.kt"
    val testFileName = "test/Tests.kt"
    val oldTaskFileText = "fun foo() {}"
    val oldTestFileText = "fun test() {}"
    val oldTaskDescriptionText = "Old Description"
    val updatedTaskFileText = "fun updated() {}"
    val updatedTestFileText = "fun updatedTest() {}"
    val updatedTaskDescriptionText = "New Description"

    val course = createCourseWithFrameworkLesson(
      taskFileName,
      testFileName,
      oldTaskFileText,
      oldTestFileText,
      oldTaskDescriptionText
    )
    course.marketplaceCourseVersion = 1

    val courseFromServer = course {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 3, taskDescription = updatedTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile(taskFileName, updatedTaskFileText)
          taskFile(testFileName, updatedTestFileText)
        }
        eduTask("task2", stepId = 4, taskDescription = updatedTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile(taskFileName, updatedTaskFileText)
          taskFile(testFileName, updatedTestFileText)
        }
      }
    } as EduCourse

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("src") {
            file("Task.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
        }
        dir("task1") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file(COURSE_AUTHOR_CONTENTS_FILE)
    }

    doTest(course, courseFromServer, expectedStructure, 2)

    val firstTask = getFirstTask(course)
    checkNotNull(firstTask)
    checkTaskFiles(firstTask, oldTaskFileText, oldTestFileText, oldTaskDescriptionText, taskFileName, testFileName)

    val allTasks = course.allTasks
    assertEquals(2, allTasks.size)
    val secondTask = allTasks[1]
    checkTaskFiles(secondTask, oldTaskFileText, oldTestFileText, oldTaskDescriptionText, taskFileName, testFileName)
  }

  private fun createCourseWithFrameworkLesson(
    taskFileName: String,
    testFileName: String,
    oldTaskFileText: String,
    oldTestFileText: String,
    oldTaskDescriptionText: String,
    firstTaskName: String = "task1",
    secondTaskName: String = "task2",
    firstTaskId: Int = 1,
    secondTaskId: Int = 2
  ): EduCourse {
    val course = course {
      frameworkLesson("lesson1") {
        eduTask(firstTaskName, stepId = firstTaskId, taskDescription = oldTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile(taskFileName, oldTaskFileText)
          taskFile(testFileName, oldTestFileText)
        }
        eduTask(secondTaskName, stepId = secondTaskId, taskDescription = oldTaskDescriptionText, taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile(taskFileName, oldTaskFileText)
          taskFile(testFileName, oldTestFileText)
        }
      }
    } as EduCourse
    return course
  }

  private fun checkTaskFiles(task: Task,
                             expectedTaskFileText: String,
                             expectedTestFileText: String,
                             expectedTaskDescriptionText: String,
                             taskFileName: String,
                             testFileName: String) {
    assertEquals(expectedTaskFileText, task.getTaskFile(taskFileName)?.text)
    assertEquals(expectedTestFileText, task.getTaskFile(testFileName)?.text)
    assertEquals(expectedTaskDescriptionText, task.descriptionText)
  }

  private fun createCourse(firstTaskStatus: CheckStatus): EduCourse {
    val course = course {
      lesson {
        eduTask(stepId = 1) {
          taskFile("TaskFile1.kt")
        }
        eduTask(stepId = 2) {
          taskFile("TaskFile2.kt")
        }
      }
    } as EduCourse
    getFirstTask(course)?.status = firstTaskStatus
    course.marketplaceCourseVersion = 1

    return course
  }

  fun doTest(course: EduCourse, courseFromServer: EduCourse, expectedFileTree: FileTree, remoteCourseVersion: Int) {
    loadCourseStructure(course, courseFromServer)
    MarketplaceCourseUpdater(project, course, remoteCourseVersion).doUpdate(courseFromServer)
    checkCourseStructure(course, courseFromServer, expectedFileTree)
    assertEquals(remoteCourseVersion, course.marketplaceCourseVersion)
  }

  private fun loadCourseStructure(course: EduCourse, courseFromServer: EduCourse) {
    setTopLevelSection(course)
    createCourseStructure(course)
    setTopLevelSection(courseFromServer)
  }

  private fun checkCourseStructure(course: EduCourse, courseFromServer: EduCourse, expectedFileTree: FileTree) {
    assertEquals("Lessons number mismatch. Expected: ${courseFromServer.lessons.size}. Actual: ${course.lessons.size}",
      courseFromServer.lessons.size, course.lessons.size)

    assertEquals("Sections number mismatch. Expected: ${courseFromServer.sections.size}. Actual: ${course.sections.size}",
      courseFromServer.sections.size, course.sections.size)

    for ((section, newSection) in course.sections.zip(courseFromServer.sections)) {
      assertTrue("Lesson number mismatch.\n" +
                 "Lesson \"${section.name}\". \n" +
                 "Expected lesson number: ${newSection.lessons.size}. Actual: ${section.lessons.size}",
        section.lessons.size == newSection.lessons.size)

      checkLessons(section.lessons, newSection.lessons)
    }

    checkLessons(course.lessons, courseFromServer.lessons)


    expectedFileTree.assertEquals(rootDir)
  }

  private fun setTopLevelSection(course: EduCourse) {
    if (course.lessons.isNotEmpty()) {
      // it's a hack.Originally we need to put here and id of remote section for top-level lesson
      course.sectionIds = Collections.singletonList(1)
    }
  }

  private fun checkLessons(lessons: List<Lesson>,
                           lessonsFromServer: List<Lesson>) {
    for ((lesson, newLesson) in lessons.zip(lessonsFromServer)) {
      assertTrue("Tasks number mismatch.\n" +
                 "Lesson \"${lesson.name}\". \n" +
                 "Expected task number: ${newLesson.taskList.size}. Actual: ${lesson.taskList.size}",
        lesson.taskList.size == newLesson.taskList.size)

      assertTrue("Lesson name mismatch. Expected: ${newLesson.name}. Actual: ${lesson.name}", lesson.name == newLesson.name)
      for ((task, newTask) in lesson.taskList.zip(newLesson.taskList)) {
        assertTrue("Task files number mismatch.\n" +
                   "Lesson \"${lesson.name}\". \n" +
                   "Task \"${task.name}\". \n" +
                   "Expected task files number: ${newTask.taskFiles.size}. Actual: ${task.taskFiles.size}",
          task.taskFiles.size == newTask.taskFiles.size)

        assertTrue("Task text mismatch.\n" +
                   "Lesson \"${lesson.name}\". \n" +
                   "Task \"${task.name}\". \n" +
                   "Expected:\n \"${newTask.descriptionText}\"\n" +
                   "Actual:\n \"${task.descriptionText}\"",
          newTask.descriptionText == task.descriptionText)

        assertTrue("Lesson index mismatch.\n Expected: Lesson \"${newLesson.name}\", index: ${newLesson.index}.\n" +
                   " Actual: Lesson \"${lesson.name}\", index: ${lesson.index}", lesson.index == newLesson.index)

      }
    }
  }
}