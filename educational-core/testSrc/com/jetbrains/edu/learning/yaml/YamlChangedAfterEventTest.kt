package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.testAction

class YamlChangedAfterEventTest : YamlTestCase() {
  fun `test hyperskill framework lesson navigation with learner created file`() {
    val course = createHyperskillFrameworkCourse(isTemplateBased = false)

    val task1 = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, project.courseDir, "lesson1/task/userFile.txt", "user file")
      task1.openTaskFileInEditor("file1.txt")
      testAction(NextTaskAction.ACTION_ID)
    }

    val expectedConfig = """
      |type: edu
      |files:
      |- name: file1.txt
      |  visible: true
      |  learner_created: true
      |- name: userFile.txt
      |  visible: true
      |  learner_created: true
      |status: Unchecked
      |record: -1
      |""".trimMargin()

    val lesson1task2 = course.findTask("lesson1", "task2")
    checkConfig(lesson1task2, expectedConfig)
    assertEquals("task 1", lesson1task2.taskFiles["file1.txt"]?.contents?.textualRepresentation)
    assertEquals("user file", lesson1task2.taskFiles["userFile.txt"]?.contents?.textualRepresentation)
  }

  private fun checkConfig(item: StudyItem, expectedConfig: String) {
    UIUtil.dispatchAllInvocationEvents()

    val configFile = item.getConfigDir(project).findChild(item.configFileName)
                     ?: error("No config file for item: ${item::class.simpleName} ${item.name}")

    val document = FileDocumentManager.getInstance().getDocument(configFile)!!
    assertEquals(expectedConfig, document.text)
  }

  @Suppress("SameParameterValue")
  private fun createHyperskillFrameworkCourse(isTemplateBased: Boolean): HyperskillCourse {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask {
          taskFile("file1.txt", "task 1")
        }
        eduTask {
          taskFile("file2.txt", "task2")
        }
        eduTask {
          taskFile("file3.txt", "task3")
        }
      }
    } as HyperskillCourse

    val hyperskillProject = HyperskillProject()
    hyperskillProject.isTemplateBased = isTemplateBased

    course.hyperskillProject = hyperskillProject
    course.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2), HyperskillStage(3, "", 3))

    createConfigFiles(project)

    return course
  }
}