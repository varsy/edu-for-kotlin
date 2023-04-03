package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import com.jetbrains.edu.learning.yaml.YamlTestCase
import java.util.*


class YamlSerializationTest : YamlTestCase() {

  fun `test edu task`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
          }
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: |-
    |      type here
    |      and here
    |  is_binary: false
    |""".trimMargin())
  }

  fun `test remote edu task`() {
    val task = course {
      lesson {
        remoteEduTask {
          taskFile("Test.java")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: remote_edu
    |files:
    |- name: Test.java
    |  visible: true
    |  is_binary: false
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test remote edu task with check profile`() {
    val checkProfile = "hyperskill_go"
    val task = course {
      lesson {
        remoteEduTask(checkProfile = checkProfile) {
          taskFile("Main.go")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: remote_edu
    |files:
    |- name: Main.go
    |  visible: true
    |  is_binary: false
    |  learner_created: false
    |check_profile: $checkProfile
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test codeforces task`() {
    val course = course(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask("first task", taskDescription = "") { }
      }
    }

    val task = course.lessons.first().taskList.first().apply {
      record = 23
      contentTags = listOf("kotlin", "cycles")
    }
    doTest(task, """
          |type: codeforces
          |status: Unchecked
          |
        """.trimMargin())
  }

  fun `test checkiO mission`() {
    val course = course(courseProducer = ::CheckiOCourse) {
      lesson {
        mission("mission") { }
      }
    }

    val task = course.lessons.first().taskList.first().apply {
      record = 23
      contentTags = listOf("kotlin", "cycles")
    }
    doTest(task, """
          |type: checkiO
          |status: Unchecked
          |seconds_from_change: 0
          |
        """.trimMargin())
  }

  fun `test edu task with content tags`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
          }
        }
      }
    }.findTask("lesson1", "task1")
    task.contentTags = listOf("kotlin", "cycles")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: |-
    |      type here
    |      and here
    |  is_binary: false
    |tags:
    |- kotlin
    |- cycles
    |""".trimMargin())
  }

  fun `test empty placeholder`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "")
          }
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: ""
    |  is_binary: false
    |""".trimMargin())
  }

  fun `test placeholder starts with spaces`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "   type here")
          }
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: '   type here'
    |  is_binary: false
    |""".trimMargin())
  }

  fun `test placeholder ends with spaces`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here   ")
          }
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: 'type here   '
    |  is_binary: false
    |""".trimMargin())
  }

  fun `test edu task with test files`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Task.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
          }
          taskFile("Test.java", "my test", false)
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Task.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: |-
    |      type here
    |      and here
    |  is_binary: false
    |- name: Test.java
    |  visible: false
    |  is_binary: false
    |""".trimMargin())
  }

  fun `test edu task with additional files`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
          }
          taskFile("Additional.java", "", false)
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: |-
    |      type here
    |      and here
    |  is_binary: false
    |- name: Additional.java
    |  visible: false
    |  is_binary: false
    |""".trimMargin())
  }

  fun `test edu task with dependency`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>f()</p>") {
            placeholder(0, "test")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("Test.java", "<p>f()</p>") {
            placeholder(0, placeholderText = "type here", dependency = "lesson1#task1#Test.java#1")
          }
        }
      }
    }.findTask("lesson2", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    dependency:
    |      lesson: lesson1
    |      task: task1
    |      file: Test.java
    |      placeholder: 1
    |      is_visible: true
    |  is_binary: false
    |""".trimMargin())
  }

  fun `test output task`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        outputTask {
          taskFile("Test.java", "")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: output
    |files:
    |- name: Test.java
    |  visible: true
    |  is_binary: false
    |""".trimMargin())
  }

  fun `test quiz task`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("Test.java", "")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
      |type: choice
      |is_multiple_choice: false
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |files:
      |- name: Test.java
      |  visible: true
      |  is_binary: false
      |local_check: true
      |""".trimMargin())
  }

  fun `test quiz task without answers`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.UNKNOWN, "2" to ChoiceOptionStatus.UNKNOWN)) {
          taskFile("Test.java", "")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
      |type: choice
      |is_multiple_choice: false
      |options:
      |- text: 1
      |- text: 2
      |files:
      |- name: Test.java
      |  visible: true
      |  is_binary: false
      |local_check: true
      |""".trimMargin())
  }

  fun `test course`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
      lesson("the first lesson")
      lesson("the second lesson")
    }
    course.languageCode = "ru"
    course.description = "This is a course about string theory.\nWhy not?"
    doTest(course, """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |
    """.trimMargin())
  }

  fun `test lesson`() {
    val lesson = course {
      lesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    doTest(lesson, """
      |content:
      |- Introduction Task
      |- Advanced Task
      |
    """.trimMargin())
  }

  fun `test lesson with custom presentable name`() {
    val lesson = course {
      lesson(customPresentableName = "my new lesson") {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    doTest(lesson, """
      |custom_name: ${lesson.customPresentableName}
      |content:
      |- Introduction Task
      |- Advanced Task
      |
    """.trimMargin())
  }

  fun `test lesson with content tags`() {
    val lesson = course {
      lesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    lesson.contentTags = listOf("kotlin", "cycles")
    @Suppress("DEPRECATION") // using `customPresentableName` here is ok
    doTest(lesson, """
      |content:
      |- Introduction Task
      |- Advanced Task
      |tags:
      |- kotlin
      |- cycles
      |
    """.trimMargin())
  }

  fun `test use dir name for lesson with custom name`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson(customPresentableName = "my new lesson") {
          eduTask("Introduction Task")
          eduTask("Advanced Task")
        }
      }
    }
    val section = course.sections.first()
    val lesson = section.lessons.first()

    doTest(section, """
      |content:
      |- ${lesson.name}
      |
    """.trimMargin())
  }

  fun `test framework lesson`() {
    val lesson = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    doTest(lesson, """
      |type: framework
      |content:
      |- Introduction Task
      |- Advanced Task
      |
    """.trimMargin())
  }

  fun `test framework lesson with content tags`() {
    val lesson = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    lesson.contentTags = listOf("kotlin", "cycles")
    doTest(lesson, """
      |type: framework
      |content:
      |- Introduction Task
      |- Advanced Task
      |tags:
      |- kotlin
      |- cycles
      |
    """.trimMargin())
  }

  fun `test non templated based framework lesson`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson", isTemplateBased = false)
    }

    doTest(course.getItem("lesson")!!, """
      |type: framework
      |is_template_based: false
      |
    """.trimMargin())
  }

  fun `test framework lesson with custom name`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson", customPresentableName = "my new lesson")
    }

    val lesson = course.getItem("lesson")!!

    doTest(lesson, """
      |type: framework
      |custom_name: my new lesson
      |
    """.trimMargin())
  }

  fun `test section`() {
    val section = course(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson("Introduction Lesson")
        lesson("Advanced Lesson")
      }
    }.items[0]

    doTest(section, """
      |content:
      |- Introduction Lesson
      |- Advanced Lesson
      |
    """.trimMargin())
  }

  fun `test section with custom name`() {
    val section = course(courseMode = CourseMode.EDUCATOR) {
      section(customPresentableName = "custom section name") {
        lesson("Introduction Lesson")
        lesson("Advanced Lesson")
      }
    }.items[0]

    doTest(section, """
      |custom_name: custom section name
      |content:
      |- Introduction Lesson
      |- Advanced Lesson
      |
    """.trimMargin())
  }

  fun `test section with content tags`() {
    val section = course(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson("Introduction Lesson")
        lesson("Advanced Lesson")
      }
    }.items[0]

    section.contentTags = listOf("kotlin", "cycles")
    doTest(section, """
      |content:
      |- Introduction Lesson
      |- Advanced Lesson
      |tags:
      |- kotlin
      |- cycles
      |
    """.trimMargin())
  }

  fun `test task feedback link`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }.findTask("lesson1", "task1")
    task.feedbackLink = "example.com"
    doTest(task, """
    |type: edu
    |feedback_link: example.com
    |""".trimMargin())
  }

  fun `test checkiO mission feedback link not serialized`() {
    val task = course(courseProducer = ::CheckiOCourse) {
      lesson {
        mission { }
      }
    }.findTask("lesson1", "task1")
    task.feedbackLink = "example.com"
    doTest(task, """
    |type: checkiO
    |status: Unchecked
    |seconds_from_change: 0
    |""".trimMargin())
  }

  fun `test task with custom presentable name`() {
    val taskCustomName = "task custom name"
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(customPresentableName = taskCustomName) { }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |""".trimMargin())
  }

  fun `test course with environment`() {
    val course = course(courseMode = CourseMode.EDUCATOR, environment = EduNames.ANDROID) {
      lesson {
        eduTask { }
      }
    }
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin())
  }

  fun `test course feedback link`() {
    val courseLink = "https://course_link.com"
    val course = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }.apply { feedbackLink = courseLink }
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |feedback_link: $courseLink
      |
    """.trimMargin())
  }

  fun `test codeforces course feedback link not serialized`() {
    val courseLink = "https://course_link.com"
    val course = course(courseProducer = ::CodeforcesCourse) {
      lesson {
        codeforcesTask { }
      }
    }.apply { feedbackLink = courseLink }
    doTest(course, """
      |type: codeforces
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |
    """.trimMargin())
  }

  fun `test codeforces course with programTypeId`() {
    val course = course(courseProducer = ::CodeforcesCourse) {} as CodeforcesCourse
    course.apply {
      languageCode = "en"
      programTypeId = "0"
    }

    doTest(course, """
      |type: codeforces
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |program_type_id: 0
      |mode: Study
      |
    """.trimMargin())
  }

  fun `test codeforces course without programTypeId`() {
    val course = course(courseProducer = ::CodeforcesCourse) {} as CodeforcesCourse
    course.apply {
      languageCode = "en"
    }

    doTest(course, """
      |type: codeforces
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |mode: Study
      |
    """.trimMargin())
  }

  fun `test coursera course feedback link not serialized`() {
    val courseLink = "https://course_link.com"
    val course = course(courseProducer = ::CourseraCourse) {
      lesson {
        eduTask { }
      }
    }.apply { feedbackLink = courseLink }
    doTest(course, """
      |type: coursera
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |
    """.trimMargin())
  }

  fun `test course with hidden solutions`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}
    course.solutionsHidden = true
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |solutions_hidden: true
      |
    """.trimMargin())
  }

  fun `test task with hidden solution`() {
    val taskCustomName = "task custom name"
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(customPresentableName = taskCustomName) {}
      }
    }.findTask("lesson1", "task1")

    task.solutionHidden = true
    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |solution_hidden: true
    |""".trimMargin("|"))
  }

  fun `test task with hidden solution = false`() {
    val taskCustomName = "task custom name"
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(customPresentableName = taskCustomName) {}
      }
    }.findTask("lesson1", "task1")

    task.solutionHidden = false
    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |solution_hidden: false
    |""".trimMargin("|"))
  }

  fun `test empty lesson`() {
    val lesson = course {
      lesson {
      }
    }.items.first()

    doTest(lesson, """
      |{}
      |
    """.trimMargin())
  }

  fun `test empty section`() {
    val section = course(courseMode = CourseMode.EDUCATOR) {
      section {
      }
    }.items.first()

    doTest(section, """
      |{}
      |
    """.trimMargin())
  }

  fun `test empty course`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}

    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |
    """.trimMargin())
  }

  fun `test course with lang version`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}
    course.languageCode = "ru"
    course.description = "sum"
    course.programmingLanguage = "${PlainTextLanguage.INSTANCE.id} 1.42"
    doTest(course, """
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |
    """.trimMargin())
  }

  fun `test course with content tags`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}
    course.contentTags = listOf("kotlin", "cycles")
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |tags:
      |- kotlin
      |- cycles
      |
    """.trimMargin())
  }

  fun `test coursera course`() {
    val course = course(courseMode = CourseMode.EDUCATOR, courseProducer = ::CourseraCourse) {}
    course.languageCode = "ru"
    course.description = "sum"
    course.programmingLanguage = "${PlainTextLanguage.INSTANCE.id} 1.42"
    doTest(course, """
      |type: coursera
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |
    """.trimMargin())
  }

  fun `test coursera course manual submit`() {
    val course = course(courseMode = CourseMode.EDUCATOR, courseProducer = ::CourseraCourse) {} as CourseraCourse
    course.languageCode = "ru"
    course.description = "sum"
    course.submitManually = true
    course.programmingLanguage = "${PlainTextLanguage.INSTANCE.id} 1.42"
    doTest(course, """
      |type: coursera
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |submit_manually: true
      |
    """.trimMargin())
  }

  fun `test course with non-english locale`() {
    val defaultLocale = Locale.getDefault()
    Locale.setDefault(Locale.KOREAN)

    val course = course(courseMode = CourseMode.EDUCATOR) {
      lesson("the first lesson")
      lesson("the second lesson")
    }
    course.languageCode = "ru"
    course.description = "This is a course about string theory.\nWhy not?"
    doTest(course, """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |
    """.trimMargin())

    Locale.setDefault(defaultLocale)
  }

  fun `test course with generatedEduId`() {
    val generatedEduId = "generated_edu_id"

    val course = course(courseMode = CourseMode.EDUCATOR) {
      lesson("the first lesson")
      lesson("the second lesson")
    } as EduCourse
    course.languageCode = "ru"
    course.generatedEduId = generatedEduId
    course.description = "This is a course about string theory.\nWhy not?"

    val actual = YamlFormatSynchronizer.REMOTE_MAPPER.writeValueAsString(course)
    val expected = """
      |id: 0
      |generated_edu_id: generated_edu_id
      |
    """.trimMargin()
    assertEquals(expected, actual)
  }

  fun `test course with Vendor`() {
    val vendor = Vendor().apply {
      name = "Vendor name"
      email = "email@gmail.com"
      url = "https://vendor.com/"
    }

    val course = course(courseMode = CourseMode.EDUCATOR) {
      lesson("the first lesson")
      lesson("the second lesson")
    }
    course.languageCode = "ru"
    course.vendor = vendor
    course.description = "This is a course about string theory.\nWhy not?"
    doTest(course, """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?
      |vendor:
      |  name: Vendor name
      |  email: email@gmail.com
      |  url: https://vendor.com/
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |
    """.trimMargin())
  }

  fun `test course with choice tasks`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        choiceTask(
          name = "task1",
          isMultipleChoice = true,
          choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT),
          messageCorrect = "You are genius!",
          messageIncorrect = "Try more",
          quizHeader = "Let's do it!",
        ) {
          taskFile("task.txt")
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
      |type: choice
      |is_multiple_choice: true
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |message_correct: You are genius!
      |message_incorrect: Try more
      |quiz_header: Let's do it!
      |files:
      |- name: task.txt
      |  visible: true
      |  is_binary: false
      |local_check: true
      |""".trimMargin())
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = item.course.mapper.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}