package com.jetbrains.edu.learning.actions.move

import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.withEduTestDialog

class MoveTest : MoveTestBase() {

  fun `test forbid section moving in student mode`() {
    val sectionName1 = "section1"
    val sectionName2 = "section2"
    val course = courseWithFiles {
      section(sectionName1)
      section(sectionName2)
    }

    val source = findPsiDirectory(sectionName1)
    val target = findPsiDirectory(sectionName2)

    withEduTestDialog(EduTestDialog()) {
      doMoveAction(course, source, target, delta = 1)
    }.checkWasShown()

    val section1 = course.getSection(sectionName1)!!
    val section2 = course.getSection(sectionName2)!!

    assertEquals(1, section1.index)
    assertEquals(2, section2.index)
  }

  fun `test forbid lesson moving in student mode`() {
    val sectionName1 = "section1"
    val sectionName2 = "section2"
    val lessonName = "lesson1"
    val course = courseWithFiles {
      section(sectionName1) {
        lesson(lessonName)
      }
      section(sectionName2)
    }

    val source = findPsiDirectory("$sectionName1/$lessonName")
    val target = findPsiDirectory(sectionName2)

    withEduTestDialog(EduTestDialog()) {
      doMoveAction(course, source, target)
    }.checkWasShown()

    val section1 = course.getSection(sectionName1)!!
    val section2 = course.getSection(sectionName2)!!

    assertNotNull(section1.getLesson(lessonName))
    assertEquals(0, section2.items.size)
  }

  fun `test forbid task moving in student mode`() {
    val lessonName1 = "lesson1"
    val lessonName2 = "lesson2"
    val taskName = "task1"
    val course = courseWithFiles {
      lesson(lessonName1) {
        eduTask(taskName)
      }
      lesson(lessonName2)
    }

    val source = findPsiDirectory("$lessonName1/$taskName")
    val target = findPsiDirectory(lessonName2)

    withEduTestDialog(EduTestDialog()) {
      doMoveAction(course, source, target)
    }.checkWasShown()

    val lesson1 = course.getLesson(lessonName1)!!
    val lesson2 = course.getLesson(lessonName2)!!

    assertNotNull(lesson1.getTask(taskName))
    assertEquals(0, lesson2.items.size)
  }

  fun `test forbid course file moving in student mode`() {
    val lessonName = "lesson1"
    val taskName = "task1"
    val taskFileName = "taskFile1.txt"
    val course = courseWithFiles {
      lesson(lessonName) {
        eduTask(taskName) {
          taskFile("src/$taskFileName")
        }
      }
    }

    val sourceFile = findPsiFile("$lessonName/$taskName/src/$taskFileName")
    val targetDir = findPsiDirectory("$lessonName/$taskName")

    withEduTestDialog(EduTestDialog()) {
      doMoveAction(course, sourceFile, targetDir)
    }.checkWasShown()

    val task = course.findTask(lessonName, taskName)

    assertNotNull(task.getTaskFile("src/$taskFileName"))
    assertNull(task.getTaskFile(taskFileName))
  }

  fun `test move learner created task file in student mode`() {
    val lessonName = "lesson1"
    val taskName = "task1"
    val course = courseWithFiles {
      lesson(lessonName) {
        eduTask(taskName)
      }
    }

    val taskFileName = "taskFile.txt"
    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, findFile("$lessonName/$taskName"), "src/$taskFileName", "")
    }

    val sourceFile = findPsiFile("$lessonName/$taskName/src/$taskFileName")
    val targetDir = findPsiDirectory("$lessonName/$taskName")

    doMoveAction(course, sourceFile, targetDir)

    val task = course.findTask(lessonName, taskName)

    assertNull(task.getTaskFile("src/$taskFileName"))
    assertNotNull(task.getTaskFile(taskFileName))
  }

  fun `test forbid task file moving to another task in student mode`() {
    val lessonName = "lesson1"
    val taskName1 = "task1"
    val taskName2 = "task2"
    val course = courseWithFiles {
      lesson(lessonName) {
        eduTask(taskName1)
        eduTask(taskName2)
      }
    }

    val taskFileName = "taskFile.txt"
    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, findFile("$lessonName/$taskName1"), taskFileName, "")
    }

    val sourceFile = findPsiFile("$lessonName/$taskName1/$taskFileName")
    val targetDir = findPsiDirectory("$lessonName/$taskName2")

    withEduTestDialog(EduTestDialog()) {
      doMoveAction(course, sourceFile, targetDir)
    }.checkWasShown()

    val task1 = course.findTask(lessonName, taskName1)
    val task2 = course.findTask(lessonName, taskName2)

    assertNotNull(task1.getTaskFile(taskFileName))
    assertNull(task2.getTaskFile(taskFileName))
  }

  fun `test move task files in CC mode 1`() {
    val lessonName = "lesson1"
    val taskName = "task1"
    val taskFileName = "taskFile1.txt"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson(lessonName) {
        eduTask(taskName) {
          taskFile("src/$taskFileName")
        }
      }
    }

    val sourceFile = findPsiFile("$lessonName/$taskName/src/$taskFileName")
    val targetDir = findPsiDirectory("$lessonName/$taskName")

    doMoveAction(course, sourceFile, targetDir)
    val task = course.findTask(lessonName, taskName)

    assertNull(task.getTaskFile("src/$taskFileName"))
    assertNotNull(task.getTaskFile(taskFileName))
  }

  fun `test move task files in CC mode 2`() {
    val lessonName = "lesson1"
    val taskName = "task1"
    val taskFileName = "taskFile1.txt"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson(lessonName) {
        eduTask(taskName) {
          taskFile(taskFileName)
          taskFile("src/taskFile2.txt")
        }
      }
    }

    val sourceFile = findPsiFile("$lessonName/$taskName/$taskFileName")
    val targetDir = findPsiDirectory("$lessonName/$taskName/src")

    doMoveAction(course, sourceFile, targetDir)
    val task = course.findTask(lessonName, taskName)

    assertNull(task.getTaskFile(taskFileName))
    assertNotNull(task.getTaskFile("src/$taskFileName"))
  }
}
