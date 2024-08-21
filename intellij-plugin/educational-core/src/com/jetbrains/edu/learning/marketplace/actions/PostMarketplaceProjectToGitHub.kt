package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.writeBytes
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.github.PostToGithubActionProvider
import com.jetbrains.edu.learning.marketplace.isMarketplaceStudentCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls

class PostMarketplaceProjectToGitHub : DumbAwareAction() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isMarketplaceStudentCourse()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project.takeIf { it != null && !it.isDisposed } ?: return
    val postToGithubActionProvider = PostToGithubActionProvider.first() ?: error("PostToGithubActionProvider not found")
    val course = project.course as? EduCourse ?: error("Marketplace course is expected")
    val courseDir = project.courseDir
    setPrompted(course.id)

    val courseFrameworkLessons = course.frameworkLessons()
    val frameworkLessonsFromSections = course.sections.flatMap { it.frameworkLessons() }.toSet()
    val frameworkLessons = courseFrameworkLessons.union(frameworkLessonsFromSections)
    if (frameworkLessons.isEmpty()) {
      LOG.info("No guided projects found for course ${course.id}")
      EduNotificationManager.showErrorNotification(
        project,
        EduCoreBundle.message("action.Educational.Student.PostMarketplaceProjectToGitHub.notification.title.failed.to.post.project.to.marketplace"),
        EduCoreBundle.message("action.Educational.Student.PostMarketplaceProjectToGitHub.notification.content.no.framework.lessons.found")
      )
      return
    }
    for (lesson in frameworkLessons) {
      val taskList = lesson.taskList
      val currentTask = lesson.currentTask() ?: taskList.first()
      val taskToNavigate = taskList.lastOrNull { it !is TheoryTask && it.isSolved } ?: currentTask
      generateTests(project, taskToNavigate)
      NavigationUtils.navigateToTask(project, taskToNavigate, currentTask, showDialogIfConflict = false)
    }
    val excludedLessons = course.lessons - frameworkLessons

    generateGitignore(courseFrameworkLessons, frameworkLessonsFromSections, excludedLessons, course, courseDir)
    generateReadme(course, courseDir)

    postToGithubActionProvider.postToGitHub(project, project.courseDir)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  /**
   * To change the content of the generated `.gitignore` file, please change the content of `github_marketplace.gitignore.ft` file
   *
   * @see fileTemplates.internal
   */
  private fun generateGitignore(
    courseFrameworkLessons: Set<FrameworkLesson>,
    sectionsFrameworkLessons: Set<FrameworkLesson>,
    excludedLessons: List<Lesson>,
    course: EduCourse,
    courseDir: VirtualFile
  ) {
    val excludeLessonsString = excludedLessons.map { it.name }.joinToString("\n") { "$it/" }
    val courseFrameworkLessonsString = courseFrameworkLessons.map { it.name }.joinToString("\n") { "$it/*/\n$it/$TASK/*/" }
    val frameworkLessonsFromSectionsString =
      sectionsFrameworkLessons.map { it.section?.name to it.name }.joinToString("\n") { (sectionName, lessonName) ->
        "$sectionName/*/\n$sectionName/$lessonName/*/\n$sectionName/$lessonName/$TASK/*/"
      }
    val courseFrameworkLessonsSolutionDirsString =
      courseFrameworkLessons.map { it.name }.joinToString("\n") { "!/${it}/$TASK/\n!/${it}/$TASK/$SRC/" }
    val frameworkLessonsFromSectionsSolutionDirsString =
      sectionsFrameworkLessons.map { it.section?.name to it.name }.joinToString("\n") { (sectionName, lessonName) ->
        "!/$sectionName/$lessonName/\n!/$sectionName/$lessonName/$TASK/\n!/$sectionName/$lessonName/$TASK/$SRC/"
      }

    val templateVariables = mapOf(
      EXCLUDED_LESSONS to excludeLessonsString,
      INCLUDED_LESSONS to "$courseFrameworkLessonsString\n$frameworkLessonsFromSectionsString",
      SOLUTION_DIRS to "$courseFrameworkLessonsSolutionDirsString\n$frameworkLessonsFromSectionsSolutionDirsString"
    )

    GeneratorUtils.createFileFromTemplate(
      CourseInfoHolder.fromCourse(course, courseDir),
      courseDir,
      GITIGNORE_FILE_PATH,
      GITIGNORE_TEMPLATE_NAME,
      templateVariables
    )
  }

  /**
   * To change the content of the generated `README.md` file, please change the content of `github_marketplace.readme.ft` file
   *
   * @see fileTemplates.internal
   */
  private fun generateReadme(course: EduCourse, courseDir: VirtualFile) {
    val templateVariables = mapOf(
      COURSE_NAME to course.presentableName
    )

    GeneratorUtils.createFileFromTemplate(
      CourseInfoHolder.fromCourse(course, courseDir),
      courseDir,
      README_FILE_PATH,
      README_TEMPLATE_NAME,
      templateVariables
    )
  }

  private fun generateTests(project: Project, task: Task) { // generate them only for the last step as we push only last step
    val invisibleTestFiles: List<TaskFile> = task.taskFiles.values.filter {
      EduUtilsKt.isTestsFile(task, it.name) && !it.isVisible && (task is EduTask || task is OutputTask)
    }
    createTests(project, invisibleTestFiles)
  }

  private fun createTests(project: Project, testFiles: List<TaskFile>) {
    invokeAndWaitIfNeeded {
      testFiles.forEach { file ->
        when (val contents = file.contents) {
          is BinaryContents -> replaceFileBytes(project, file, contents.bytes)
          is TextualContents -> replaceFileText(project, file, contents.text)
          is UndeterminedContents -> replaceFileText(project, file, contents.textualRepresentation)
        }
      }
    }
  }

  private fun replaceFileBytes(project: Project, file: TaskFile, bytes: ByteArray) {
    CommandProcessor.getInstance().runUndoTransparentAction {
      runWriteAction {
        file.getVirtualFile(project)?.writeBytes(bytes)
      }
    }
  }

  private fun replaceFileText(project: Project, file: TaskFile, newText: String) {
    val newDocumentText = StringUtil.convertLineSeparators(newText)
    CommandProcessor.getInstance().runUndoTransparentAction {
      runWriteAction {
        val document = file.getDocument(project) ?: return@runWriteAction
        CommandProcessor.getInstance().executeCommand(
          project,
          { document.setText(newDocumentText) },
          EduCoreBundle.message("action.change.test.text"),
          "Edu Actions"
        )
        PsiDocumentManager.getInstance(project).commitAllDocuments()
      }
    }
  }

  companion object {
    private val LOG: Logger = logger<PostMarketplaceProjectToGitHub>()

    @NonNls
    private const val ACTION_ID: String = "Educational.Student.PostMarketplaceProjectToGitHub"

    @NonNls
    private const val COMPLETION_CONDITION_REGISTRY_KEY: String = "edu.marketplace.PostMarketplaceProjectToGitHub.completion.condition"

    @NonNls
    private const val IS_PROMPTED = "com.jetbrains.edu.Marketplace.PostToGithub.Prompt.Count"

    private const val GITIGNORE_FILE_PATH: String = ".gitignore"
    private const val GITIGNORE_TEMPLATE_NAME: String = "github_marketplace.gitignore"
    private const val README_FILE_PATH: String = "README.md"
    private const val README_TEMPLATE_NAME: String = "github_marketplace.readme"

    /**
     * Template variables for `github_marketplace.gitignore.ft` and `github_marketplace.readme.ft`
     * @see fileTemplates.internal
     */
    private const val EXCLUDED_LESSONS: String = "excludedLessons"
    private const val COURSE_NAME: String = "courseName"
    private const val INCLUDED_LESSONS: String = "includedLessons"
    private const val SOLUTION_DIRS: String = "solutionDirs"

    private const val SRC: String = "src"
    private const val TASK: String = "task"

    private val COMPLETION_CONDITION: Int
      get() = Registry.intValue(COMPLETION_CONDITION_REGISTRY_KEY).coerceIn(0, 100)

    fun promptToPostProject(project: Project) {
      val course = project.course as? EduCourse ?: return
      if (!isPrompted(course.id) && canBePrompted(course)) {
        val inlineBanner = createInlineBanner()
        TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
      }
    }

    private fun LessonContainer.frameworkLessons(): Set<FrameworkLesson> {
      return lessons.filterIsInstance<FrameworkLesson>().toSet()
    }

    /**
     * Since the action is only available for courses with FrameworkLessons, we do not prompt it for courses devoid of such lessons
     */
    private fun canBePrompted(course: EduCourse): Boolean {
      val tasks = course.frameworkLessons().flatMap { it.taskList }.filter { it !is TheoryTask }
      if (tasks.isEmpty()) {
        return false
      }
      val numberOfSolvedTasks = tasks.count { it.isSolved }
      return 100 * numberOfSolvedTasks.toDouble() / tasks.size >= COMPLETION_CONDITION
    }

    private fun isPrompted(courseId: Int): Boolean {
      return PropertiesComponent.getInstance().getBoolean("$IS_PROMPTED.$courseId", false)
    }

    private fun setPrompted(courseId: Int) {
      PropertiesComponent.getInstance().setValue("$IS_PROMPTED.$courseId", true)
    }

    private fun createInlineBanner(): InlineBanner = InlineBanner(EditorNotificationPanel.Status.Info).apply {
      setMessage(EduCoreBundle.message("action.Educational.Student.PostMarketplaceProjectToGitHub.banner.message"))
      addAction(EduCoreBundle.message("action.Educational.Student.PostMarketplaceProjectToGitHub.banner.action")) {
        val action = ActionManager.getInstance().getAction(ACTION_ID)
        ActionManager.getInstance().tryToExecute(action, null, null, null, false)
        removeFromParent()
      }
    }
  }
}
