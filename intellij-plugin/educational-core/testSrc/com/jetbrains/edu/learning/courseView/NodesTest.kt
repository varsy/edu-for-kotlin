// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.edu.learning.courseView

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.junit.Test

class NodesTest : CourseViewTestBase() {

  @Test
  fun testOutsideScrDir() {
    courseWithFiles(language = FakeGradleBasedLanguage) {
      lesson {
        eduTask {
          taskFile("src/file.txt")
          taskFile("test/file.txt")
        }

        eduTask {
          taskFile("src/file.txt")
          taskFile("file1.txt")
          taskFile("test/file.txt")
        }
      }
    }

    assertCourseView("""
    |-Project
    | -CourseNode Test Course  0/2
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    file.txt
    |   -TaskNode task2
    |    -DirectoryNode src
    |     file.txt
    |    file1.txt
    """.trimMargin("|"))
  }

  @Test
  fun testSections() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt")
        }
        eduTask {
          taskFile("taskFile3.txt")
        }
        eduTask {
          taskFile("taskFile4.txt")
        }
      }
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
          eduTask {
            taskFile("taskFile2.txt")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }

    assertCourseView("""
    |-Project
    | -CourseNode Test Course  0/10
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    taskFile1.txt
    |   -TaskNode task2
    |    taskFile2.txt
    |   -TaskNode task3
    |    taskFile3.txt
    |   -TaskNode task4
    |    taskFile4.txt
    |  -SectionNode section2
    |   -LessonNode lesson1
    |    -TaskNode task1
    |     taskFile1.txt
    |    -TaskNode task2
    |     taskFile1.txt
    |   -LessonNode lesson2
    |    -TaskNode task1
    |     taskFile1.txt
    |    -TaskNode task2
    |     taskFile2.txt
    |  -LessonNode lesson2
    |   -TaskNode task1
    |    taskFile1.txt
    |   -TaskNode task2
    |    taskFile2.txt
    """.trimMargin("|"))
  }

  @Test
  fun testTaskFilesOrder() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("C.txt")
          taskFile("B.txt")
          taskFile("A.txt")
        }

        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }

    assertCourseView("""
    |-Project
    | -CourseNode Test Course  0/2
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    C.txt
    |    B.txt
    |    A.txt
    |   -TaskNode task2
    |    taskFile.txt
    """.trimMargin("|"))
  }

  @Test
  fun `test invisible files in student mode`() {
    courseWithInvisibleItems(CourseMode.STUDENT)
    assertCourseView("""
      -Project
       -CourseNode Test Course  0/2
        -LessonNode lesson1
         -TaskNode task1
          -DirectoryNode folder1
           taskFile3.txt
          taskFile1.txt
         -TaskNode task2
          -DirectoryNode folder
           additionalFile3.txt
          additionalFile1.txt
    """.trimIndent())
  }

  @Test
  fun `test invisible files in educator mode`() {
    courseWithInvisibleItems(CourseMode.EDUCATOR)
    assertCourseView("""
      -Project
       -CCCourseNode Test Course (Course Creation)
        -CCLessonNode lesson1
         -CCTaskNode task1
          -CCNode folder1
           taskFile3.txt
           CCStudentInvisibleFileNode taskFile4.txt
          CCStudentInvisibleFileNode task.md
          taskFile1.txt
          CCStudentInvisibleFileNode taskFile2.txt
         -CCTaskNode task2
          -CCNode folder
           additionalFile3.txt
           CCStudentInvisibleFileNode additionalFile4.txt
          additionalFile1.txt
          CCStudentInvisibleFileNode additionalFile2.txt
          CCStudentInvisibleFileNode task.md
    """.trimIndent())
  }

  @Test
  fun `test non course files`() {
    courseWithInvisibleItems(CourseMode.EDUCATOR)
    nonAdditionalFile("non_course_file1.txt")
    nonAdditionalFile("lesson1/task1/non_course_file2.txt")
    nonAdditionalFile("lesson1/task2/folder/non_course_file3.txt")

    assertCourseView("""
      -Project
       -CCCourseNode Test Course (Course Creation)
        -CCLessonNode lesson1
         -CCTaskNode task1
          -CCNode folder1
           taskFile3.txt
           CCStudentInvisibleFileNode taskFile4.txt
          CCStudentInvisibleFileNode non_course_file2.txt (excluded)
          CCStudentInvisibleFileNode task.md
          taskFile1.txt
          CCStudentInvisibleFileNode taskFile2.txt
         -CCTaskNode task2
          -CCNode folder
           additionalFile3.txt
           CCStudentInvisibleFileNode additionalFile4.txt
           CCStudentInvisibleFileNode non_course_file3.txt (excluded)
          additionalFile1.txt
          CCStudentInvisibleFileNode additionalFile2.txt
          CCStudentInvisibleFileNode task.md
        CCStudentInvisibleFileNode non_course_file1.txt (excluded)
    """.trimIndent())
  }

  private fun courseWithInvisibleItems(courseMode: CourseMode) {
    courseWithFiles(courseMode = courseMode) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
          taskFile("taskFile2.txt", visible = false)
          dir("folder1") {
            taskFile("taskFile3.txt")
            taskFile("taskFile4.txt", visible = false)
          }
        }
        eduTask {
          taskFile("additionalFile1.txt")
          taskFile("additionalFile2.txt", visible = false)
          dir("folder") {
            taskFile("additionalFile3.txt")
            taskFile("additionalFile4.txt", visible = false)
          }
        }
      }
    }
  }

  private fun createCourseWithTestsInsideTestDir(courseMode: CourseMode = CourseMode.STUDENT) {
    courseWithFiles(courseMode = courseMode) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
          taskFile("taskFile2.txt")
          dir("tests") {
            taskFile("Tests.txt", visible = false)
          }
        }
        eduTask {
          taskFile("additionalFile1.txt")
          taskFile("additionalFile2.txt")
          dir("folder") {
            taskFile("additionalFile3.txt")
            taskFile("additionalFile4.txt", visible = false)
          }
        }
      }
    }
  }

  @Test
  fun `test course with tests inside test dir`() {
    createCourseWithTestsInsideTestDir(CourseMode.EDUCATOR)
    assertCourseView("""   
      |-Project
      | -CCCourseNode Test Course (Course Creation)
      |  -CCLessonNode lesson1
      |   -CCTaskNode task1
      |    -CCNode tests
      |     CCStudentInvisibleFileNode Tests.txt
      |    CCStudentInvisibleFileNode task.md
      |    taskFile1.txt
      |    taskFile2.txt
      |   -CCTaskNode task2
      |    -CCNode folder
      |     additionalFile3.txt
      |     CCStudentInvisibleFileNode additionalFile4.txt
      |    additionalFile1.txt
      |    additionalFile2.txt
      |    CCStudentInvisibleFileNode task.md
    """.trimMargin("|"))
  }

  @Test
  fun `test student course with tests inside test dir`() {
    createCourseWithTestsInsideTestDir()
    assertCourseView("""
      |-Project
      | -CourseNode Test Course  0/2
      |  -LessonNode lesson1
      |   -TaskNode task1
      |    taskFile1.txt
      |    taskFile2.txt
      |   -TaskNode task2
      |    -DirectoryNode folder
      |     additionalFile3.txt
      |    additionalFile1.txt
      |    additionalFile2.txt
    """.trimMargin("|"))
  }


  @Test
  fun `test course with dir inside test`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
          taskFile("taskFile2.txt")
          dir("tests") {
            dir("package") {
              taskFile("Tests.txt", visible = false)
            }
            taskFile("Tests.txt", visible = false)
          }
        }
        eduTask {
          taskFile("additionalFile1.txt")
          taskFile("additionalFile2.txt")
          dir("folder") {
            taskFile("additionalFile3.txt")
            taskFile("additionalFile4.txt", visible = false)
          }
        }
      }
    }
    assertCourseView("""
      |-Project
      | -CCCourseNode Test Course (Course Creation)
      |  -CCLessonNode lesson1
      |   -CCTaskNode task1
      |    -CCNode tests
      |     -CCNode package
      |      CCStudentInvisibleFileNode Tests.txt
      |     CCStudentInvisibleFileNode Tests.txt
      |    CCStudentInvisibleFileNode task.md
      |    taskFile1.txt
      |    taskFile2.txt
      |   -CCTaskNode task2
      |    -CCNode folder
      |     additionalFile3.txt
      |     CCStudentInvisibleFileNode additionalFile4.txt
      |    additionalFile1.txt
      |    additionalFile2.txt
      |    CCStudentInvisibleFileNode task.md
    """.trimMargin("|"))
  }

  @Test
  fun `test directory inside lesson in educator mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
        }
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
        }
      }
    }
    runWriteAction {
      findFile("lesson1").createChildDirectory(NodesTest::class.java, "non-task")
    }
    assertCourseView("""
      |-Project
      | -CCCourseNode Test Course (Course Creation)
      |  -CCLessonNode lesson1
      |   -CCTaskNode task1
      |    file1.txt
      |    file2.txt
      |    CCStudentInvisibleFileNode task.md
      |   -CCTaskNode task2
      |    file1.txt
      |    file2.txt
      |    CCStudentInvisibleFileNode task.md
      |   CCNode non-task
    """.trimMargin("|"))
  }

  @Test
  fun `test directory inside course in educator mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
        }
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
        }
      }
    }
    runWriteAction {
      LightPlatformTestCase.getSourceRoot().createChildDirectory(NodesTest::class.java, "non-lesson")
    }
    assertCourseView("""
      |-Project
      | -CCCourseNode Test Course (Course Creation)
      |  -CCLessonNode lesson1
      |   -CCTaskNode task1
      |    file1.txt
      |    file2.txt
      |    CCStudentInvisibleFileNode task.md
      |   -CCTaskNode task2
      |    file1.txt
      |    file2.txt
      |    CCStudentInvisibleFileNode task.md
      |  CCNode non-lesson
    """.trimMargin("|"))
  }

  @Test
  fun `test excluded files in educator mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
        }
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
        }
      }
    }
    nonAdditionalFile("lesson1/LessonIgnoredFile.txt")
    nonAdditionalFile("IgnoredFile.txt")
    assertCourseView("""
      |-Project
      | -CCCourseNode Test Course (Course Creation)
      |  -CCLessonNode lesson1
      |   -CCTaskNode task1
      |    file1.txt
      |    file2.txt
      |    CCStudentInvisibleFileNode task.md
      |   -CCTaskNode task2
      |    file1.txt
      |    file2.txt
      |    CCStudentInvisibleFileNode task.md
      |   CCStudentInvisibleFileNode LessonIgnoredFile.txt (excluded)
      |  CCStudentInvisibleFileNode IgnoredFile.txt (excluded)
    """.trimMargin("|"))
  }

  @Test
  fun `test excluded files in educator mode - do not show dirs inside tasks as excluded`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
          dir("dir-inside-task-ignored") {
            taskFile("a.txt")
          }
          dir("dir-inside-task-not-ignored") {
            taskFile("c.txt")
          }
        }
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
        }
      }
      additionalFile("dir-not-ignored/a.txt")
      additionalFile("lesson1/task1/dir-inside-task-not-ignored/d.txt") // what if we put an additional file to a task folder
    }
    nonAdditionalFile("lesson1/task1/dir-inside-task-ignored/b.txt")
    nonAdditionalFile("dir-ignored/a.txt")
    nonAdditionalFile("dir-not-ignored/ignored-file.txt")
    assertCourseView("""
      |-Project
      | -CCCourseNode Test Course (Course Creation)
      |  -CCLessonNode lesson1
      |   -CCTaskNode task1
      |    -CCNode dir-inside-task-ignored
      |     a.txt
      |     CCStudentInvisibleFileNode b.txt (excluded)
      |    -CCNode dir-inside-task-not-ignored
      |     c.txt
      |     CCStudentInvisibleFileNode d.txt (excluded)
      |    file1.txt
      |    file2.txt
      |    CCStudentInvisibleFileNode task.md
      |   -CCTaskNode task2
      |    file1.txt
      |    file2.txt
      |    CCStudentInvisibleFileNode task.md
      |  -CCNode dir-ignored
      |   CCStudentInvisibleFileNode a.txt (excluded)
      |  -CCNode dir-not-ignored
      |   CCStudentInvisibleFileNode a.txt
      |   CCStudentInvisibleFileNode ignored-file.txt (excluded)
    """.trimMargin("|"))
  }

  @Test
  fun `test excluded files in educator mode - files are excluded differently inside and outside of tasks`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask {
          taskFile("ignored.txt")
          taskFile("not-ignored.txt")
        }
      }
      additionalFile("not-ignored.txt")
      additionalFile("subfolder/not-ignored.txt")
    }
    nonAdditionalFile("ignored.txt")
    nonAdditionalFile("subfolder/ignored.txt")
    nonAdditionalFile("lesson1/task1/ignored.txt")
    assertCourseView("""
      |-Project
      | -CCCourseNode Test Course (Course Creation)
      |  -CCLessonNode lesson1
      |   -CCTaskNode task1
      |    ignored.txt
      |    not-ignored.txt
      |    CCStudentInvisibleFileNode task.md
      |  -CCNode subfolder
      |   CCStudentInvisibleFileNode ignored.txt (excluded)
      |   CCStudentInvisibleFileNode not-ignored.txt
      |  CCStudentInvisibleFileNode ignored.txt (excluded)
      |  CCStudentInvisibleFileNode not-ignored.txt
    """.trimMargin("|"))
  }

  @Test
  fun `test hyperskill course`() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson  {
        eduTask {
          taskFile("file1.txt")
        }
        eduTask {
          taskFile("file2.txt")
        }
      }

      lesson {
        eduTask {
          taskFile("task1.txt")
        }
      }
    }

    findTask(0, 0).status = CheckStatus.Solved

    assertCourseView("""
      |-Project
      | -CourseNode Test Course
      |  -FrameworkLessonNode lesson1 1 of 2 stages completed
      |   file1.txt
      |  -LessonNode lesson2
      |   -TaskNode task1
      |    task1.txt
    """.trimMargin())
  }

  @Test
  fun `test hyperskill course with empty framework lesson`() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
      }
    }

    assertCourseView("""
      |-Project
      | -CourseNode Test Course
      |  FrameworkLessonNode lesson1
    """.trimMargin())
  }

  @Test
  fun `test edu course with empty framework lesson`() {
    courseWithFiles(courseProducer = ::EduCourse) {
      frameworkLesson {
      }
    }

    assertCourseView("""
      |-Project
      | -CourseNode Test Course  1/1
      |  FrameworkLessonNode lesson1
    """.trimMargin())
  }



  @Test
  fun `test course dirs first order`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("XtaskFile.txt")
          taskFile("AtaskFile.txt")
          dir("x") {
            taskFile("xTest.txt")
          }
          dir("a") {
            taskFile("aTest.txt")
          }
          dir("test") {
            dir("package") {
              taskFile("packageTest.txt")
            }
            taskFile("testTest.txt")
          }
        }
      }
    }
    assertCourseView("""
      |-Project
      | -CCCourseNode Test Course (Course Creation)
      |  -CCLessonNode lesson1
      |   -CCTaskNode task1
      |    -CCNode a
      |     aTest.txt
      |    -CCNode test
      |     -CCNode package
      |      packageTest.txt
      |     testTest.txt
      |    -CCNode x
      |     xTest.txt
      |    AtaskFile.txt
      |    CCStudentInvisibleFileNode task.md
      |    XtaskFile.txt
    """.trimMargin("|"))
  }

  /**
   * Creates a file on disk that should not go to the list of course additional files
   */
  private fun nonAdditionalFile(path: String, contents: String = "") = runWriteAction {
    project.courseDir.findOrCreateFile(path).writeText(contents)
  }
}
