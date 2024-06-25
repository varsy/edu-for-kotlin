package com.jetbrains.edu.jarvis.tutorial

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.jarvis.messages.EduJarvisBundle
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.joinPaths
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.jarvis.tutorial.TaskType.EDU_TASK
import com.jetbrains.edu.jarvis.tutorial.TaskType.THEORY_TASK

/**
 * TutorialGenerator class is used to generate a tutorial section in a course.
 * The section includes lessons designed to assist users in learning the new syntax for programming in the native language.
 * The tutorial section can be accessed by right-clicking on the project root directory (main folder) and navigating to New -> Tutorial.
 */
// TODO: change the icon
class TutorialGenerator : DumbAwareAction(EduJarvisBundle.message("item.tutorial.title"), "", EducationalCoreIcons.Section) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    val itemDir = generateTutorial(course)
    ProjectView.getInstance(project).select(itemDir, itemDir, true)
  }

  override fun update(event: AnActionEvent) {
    val presentation = event.presentation
    presentation.isEnabledAndVisible = false
    val project = event.getData(CommonDataKeys.PROJECT) ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    if (!CCUtils.isCourseCreator(project)) return
    if (course.languageId != KOTLIN) return
    val selectedFiles = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
    if (selectedFiles[0] != project.courseDir) return
    if (CommonDataKeys.PSI_FILE.getData(event.dataContext) != null) return
    presentation.isEnabledAndVisible = true
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun generateTutorial(course: Course): VirtualFile {
    val section = course.createSection()
    tutorialComponents.forEach { tutorialLesson ->
      section.createLesson(tutorialLesson.name) { lesson ->
        tutorialLesson.tasks.mapIndexed { index, tutorialTask ->
          createTask(tutorialTask.name, lesson, index + 1, tutorialTask.type) { task ->
            mutableListOf(createTaskFile(task)).apply {
              if (tutorialTask.type == EDU_TASK) {
                add(createTaskFile(task, true))
              }
            }
          }
        }
      }
    }
    val project = course.project ?: error("Project is not found")
    val virtualFile = GeneratorUtils.createSection(project, section, project.courseDir)
    section.lessons.forEach { it.taskList.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) } }
    section.lessons.forEach { lesson -> YamlFormatSynchronizer.saveItem(lesson) }
    YamlFormatSynchronizer.saveItem(section)
    YamlFormatSynchronizer.saveItem(course)
    return virtualFile
  }

  private fun Course.createSection() =
    Section().apply {
      val course = this@createSection
      name = EduJarvisBundle.message("item.tutorial.title")
      index = 0
      parent = course
      course.addItem(0, this)
    }

  private fun Section.createLesson(lessonName: String, createTasks: (Lesson) -> List<Task>) =
    FrameworkLesson().apply {
      val section = this@createLesson
      name = lessonName
      customPresentableName = lessonName.getPresentableName()
      index = section.items.size + 1
      parent = section
      section.addLesson(this)
      createTasks(this).forEach { task -> addTask(task) }
    }

  private fun createTask(taskName: String, lesson: ItemContainer, taskIndex: Int, itemType: TaskType, createTaskFiles: (Task) -> List<TaskFile>) =
    when (itemType) {
      THEORY_TASK -> TheoryTask()
      EDU_TASK -> EduTask()
    }.apply {
      name = taskName
      index = taskIndex
      customPresentableName = "${lesson.parent.presentableName} - ${lesson.presentableName} - ${taskName.getPresentableName()}"
      parent = lesson
      descriptionText = getFileContentFromResources(lesson.course, joinPaths(RESOURCE_FOLDER, lesson.name, taskName, TASK_MD))
      descriptionFormat = DescriptionFormat.MD
      taskFiles = createTaskFiles(this).associateBy { it.name }
    }

  private fun createTaskFile(task: Task, isTests: Boolean = false): TaskFile {
    val fileName = getFileName(task.course, isTests)
    return TaskFile().apply {
      name = getPath(task, fileName, isTests)
      contents = InMemoryTextualContents(getFileContentFromResources(
        task.course,
        joinPaths(RESOURCE_FOLDER, task.lesson.name, task.name, fileName)
      ))
      isVisible = !isTests
      this.task = task
    }
  }

  private fun getFileName(course: Course, isTests: Boolean) = if (isTests) {
    course.configurator?.courseBuilder?.testTemplateName(course) ?: error("Test file name not configured")
  } else {
    course.configurator?.courseBuilder?.mainTemplateName(course) ?: error("Main file name not configured")
  }

  private fun getPath(task: Task, fileName: String, isTests: Boolean) = if (isTests) {
    joinPaths(task.course.configurator?.testDirs?.firstOrNull(), fileName)
  } else {
    joinPaths(
      task.course.configurator?.sourceDir ?: error("Can't find source dir for task ${task.name}"),
      PACKAGE + task.lesson.name.toPackageFormat(),
      fileName
    )
  }

  private fun getFileContentFromResources(course: Course, path: String) = ApplicationManager.getApplication().runReadAction<String> {
    course.configurator?.javaClass?.getResource(path)
      ?.let { resourceUrl -> VfsUtil.findFileByURL(resourceUrl) }
      ?.let { virtualFile -> VfsUtil.loadText(virtualFile) }
  } ?: error("File from resources not found or unable to read: $path")

  private fun String.getPresentableName() = replace(capitalLetterRegex, " $1").trim()

  private fun String.toPackageFormat() = replace(capitalLetterRegex, "/$1").lowercase()

  companion object {
    private const val RESOURCE_FOLDER = "/jarvis/Tutorial"
    private const val PACKAGE = "jetbrains/course/tutorial"
    private val capitalLetterRegex = Regex("([A-Z])")
    val tutorialComponents = listOf(
      TutorialLesson("Introduction", listOf(TutorialTask("TutorialIntroduction", THEORY_TASK))),
      TutorialLesson("DogYears", listOf (
        TutorialTask("Introduction", THEORY_TASK),
        TutorialTask("Function", EDU_TASK),
        TutorialTask("DescriptionBlock", EDU_TASK),
        TutorialTask("ReceivingUserInput", EDU_TASK),
        TutorialTask("VariableDeclaration", EDU_TASK),
        TutorialTask("FunctionCall", EDU_TASK),
        TutorialTask("IfExpression", EDU_TASK),
        TutorialTask("Run", EDU_TASK),
        TutorialTask("Correction", EDU_TASK),
        TutorialTask("AcceptanceOfCode", EDU_TASK)
      ))
    )
  }
}
