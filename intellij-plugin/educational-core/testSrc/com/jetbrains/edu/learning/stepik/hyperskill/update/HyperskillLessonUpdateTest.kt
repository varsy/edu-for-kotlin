package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.LessonContainer
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.update.LessonUpdateTestBase
import com.jetbrains.edu.learning.update.LessonUpdater
import org.junit.Test

class HyperskillLessonUpdateTest : LessonUpdateTestBase<HyperskillCourse>() {
  override fun getUpdater(container: LessonContainer): LessonUpdater = HyperskillLessonUpdater(project, container)

  @Test
  fun `test new lesson in course created`() {
    initiateLocalCourse()
    val newEduTask = EduTask("task3").apply {
      id = 3
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests1.kt" to TaskFile("test/Tests3.kt", "fun test3() {}")
      )
      descriptionFormat = DescriptionFormat.HTML
    }
    val newLesson = Lesson().apply {
      name = "lesson2"
      addTask(newEduTask)
      newEduTask.parent = this
    }
    val newStages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
      HyperskillStage(3, "", 3)
    )
    val remoteCourse = toRemoteCourse {
      addLesson(newLesson)
      newLesson.parent = this
      stages = newStages
    }
    updateLessons(remoteCourse)

    assertEquals("Lesson hasn't been added", 2, getLessons().size)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests1.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests2.kt")
          }
          file("task.html")
        }
      }
      dir("lesson2") {
        dir("task3") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests3.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new lesson in section created`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests1.kt", "fun test1() {}")
          }
          eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests2.kt", "fun test2() {}")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject()
    localCourse.stages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
    )

    val newEduTask = EduTask("task3").apply {
      id = 3
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests1.kt" to TaskFile("test/Tests3.kt", "fun test3() {}")
      )
      descriptionFormat = DescriptionFormat.HTML
    }
    val newLesson = Lesson().apply {
      id = 2
      name = "lesson2"
      addTask(newEduTask)
      newEduTask.parent = this
    }
    val newStages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
      HyperskillStage(3, "", 3)
    )
    val remoteCourse = toRemoteCourse {
      val section = sections.first()
      section.addLesson(newLesson)
      newLesson.parent = section
      stages = newStages
    }
    updateLessons(remoteCourse, localCourse.sections.first(), remoteCourse.sections.first())

    assertEquals("Lesson hasn't been added", 2, getLessons(localCourse.sections.first()).size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests1.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests2.kt")
            }
            file("task.html")
          }
        }
        dir("lesson2") {
          dir("task3") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests3.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test first task deleted`() {
    initiateLocalCourse()
    val newStages = listOf(
      HyperskillStage(2, "", 2),
    )
    val remoteCourse = toRemoteCourse {
      lessons.first().apply {
        removeTask(taskList[0])
      }
      stages = newStages
    }
    updateLessons(remoteCourse)

    assertEquals("Task hasn't been deleted", 1, findLesson(0).taskList.size)
    assertEquals("Task index hasn't been changed", 1, findLesson(0).taskList.first().index)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests2.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test lesson in course has been deleted`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun test1() {}")
        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun test2() {}")
        }
      }
      lesson("lesson2", id = 2) {
        eduTask("task3", stepId = 3, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests3.kt", "fun test3() {}")
        }
        eduTask("task4", stepId = 4, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests4.kt", "fun test4() {}")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject()
    localCourse.stages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
      HyperskillStage(3, "", 3),
      HyperskillStage(4, "", 4)
    )

    val remoteCourse = toRemoteCourse {
      removeLesson(lessons.first())
      stages = listOf(
        HyperskillStage(3, "", 3),
        HyperskillStage(4, "", 4)
      )
    }
    updateLessons(remoteCourse)

    assertEquals("Lesson hasn't been deleted", 1, getLessons().size)

    val expectedStructure = fileTree {
      dir("lesson2") {
        dir("task3") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests3.kt")
          }
          file("task.html")
        }
        dir("task4") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests4.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test lesson in section has been deleted`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests1.kt", "fun test1() {}")
          }
          eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests2.kt", "fun test2() {}")
          }
        }
        lesson("lesson2", id = 2) {
          eduTask("task3", stepId = 3, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests3.kt", "fun test3() {}")
          }
          eduTask("task4", stepId = 4, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests4.kt", "fun test4() {}")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject()
    localCourse.stages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
      HyperskillStage(3, "", 3),
      HyperskillStage(4, "", 4)
    )

    val remoteCourse = toRemoteCourse {
      val section = sections.first()
      section.removeLesson(section.lessons.first())
      stages = listOf(
        HyperskillStage(3, "", 3),
        HyperskillStage(4, "", 4)
      )
    }
    updateLessons(remoteCourse, localCourse.sections.first(), remoteCourse.sections.first())

    assertEquals("Lesson hasn't been deleted", 1, getLessons(localCourse.sections.first()).size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson2") {
          dir("task3") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests3.kt")
            }
            file("task.html")
          }
          dir("task4") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests4.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new lesson in section created and existing task has been changed`() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section("section1", id = 1) {
        lesson("lesson1", id = 1) {
          eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests1.kt", "fun test1() {}")
          }
          eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
            taskFile("src/Task.kt", "fun foo() {}")
            taskFile("src/Baz.kt", "fun baz() {}")
            taskFile("test/Tests2.kt", "fun test2() {}")
          }
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject()
    localCourse.stages = listOf(
      HyperskillStage(1, "", 1, true),
      HyperskillStage(2, "", 2),
    )

    val updatedNameForTask1 = "task1 updated"
    val newEduTask = EduTask("task3").apply {
      id = 3
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests1.kt" to TaskFile("test/Tests3.kt", "fun test3() {}")
      )
      descriptionFormat = DescriptionFormat.HTML
    }
    val newLesson = Lesson().apply {
      id = 2
      name = "lesson2"
      addTask(newEduTask)
      newEduTask.parent = this
    }
    val remoteCourse = toRemoteCourse {
      val section = sections.first()
      section.getLessonById(1)?.getTaskById(1)?.name = updatedNameForTask1
      section.addLesson(newLesson)
      newLesson.parent = section
    }

    val firstLocalSection = localCourse.sections.first()
    updateLessons(remoteCourse, firstLocalSection, remoteCourse.sections.first())


    val actualTaskName = firstLocalSection.getLessonById(1)?.getTaskById(1)?.name
    assertEquals("Task name not updated", updatedNameForTask1, actualTaskName)
    assertEquals("Lesson hasn't been added", 2, firstLocalSection.lessons.size)

    val expectedStructure = fileTree {
      dir("section1") {
        dir("lesson1") {
          dir(updatedNameForTask1) {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests1.kt")
            }
            file("task.html")
          }
          dir("task2") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests2.kt")
            }
            file("task.html")
          }
        }
        dir("lesson2") {
          dir("task3") {
            dir("src") {
              file("Task.kt")
              file("Baz.kt")
            }
            dir("test") {
              file("Tests3.kt")
            }
            file("task.html")
          }
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  override fun initiateLocalCourse() {
    localCourse = courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun test1() {}")
        }
        eduTask("task2", stepId = 2, taskDescription = "Old Description", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests2.kt", "fun test2() {}")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    } as HyperskillCourse
    localCourse.hyperskillProject = HyperskillProject()
    localCourse.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))
  }
}