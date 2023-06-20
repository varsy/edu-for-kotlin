package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.UnsupportedTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillLanguages
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.eduEnvironment
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.stepik.showUpdateAvailableNotification
import com.jetbrains.edu.learning.submissions.isSignificantlyAfter
import com.jetbrains.edu.learning.update.UpdateUtils.shouldFrameworkLessonBeUpdated
import com.jetbrains.edu.learning.update.UpdateUtils.showUpdateCompletedNotification
import com.jetbrains.edu.learning.update.UpdateUtils.updateFrameworkLessonFiles
import com.jetbrains.edu.learning.update.UpdateUtils.updateTaskDescription
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.IOException
import java.util.*

class HyperskillCourseUpdater(private val project: Project, val course: HyperskillCourse) {
  private fun HyperskillProject.getCourseFromServer(): HyperskillCourse? {
    val connector = HyperskillConnector.getInstance()
    val hyperskillProject = when (val response = connector.getProject(id)) {
      is Err -> return null
      is Ok -> response.value
    }
    val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillProject.language) ?: return null
    val eduEnvironment = hyperskillProject.eduEnvironment ?: return null
    val stagesFromServer = connector.getStages(id) ?: return null
    return HyperskillCourse(hyperskillProject, languageId, languageVersion, eduEnvironment).apply {
      stages = stagesFromServer
      val lessonFromServer = connector.getLesson(this, hyperskillProject.ideFiles)
      addLesson(lessonFromServer)
      init(this, false)
    }
  }

  fun updateCourse(onFinish: (isUpdated: Boolean) -> Unit) {
    fun getProblemsUpdate(): List<TaskUpdate> {
      val legacyProblemLesson = course.getProblemsLesson()
      val newProblemLessons = course.getTopicsSection()?.lessons ?: emptyList()
      val problemLessons = listOfNotNull(legacyProblemLesson, *newProblemLessons.toTypedArray())
      return problemLessons.flatMap { lesson -> lesson.getProblemsUpdates() }
    }

    runInBackground(project, EduCoreBundle.message("update.check")) {
      val projectLesson = course.getProjectLesson()
      val courseFromServer = course.hyperskillProject?.getCourseFromServer()
      val hyperskillProject = course.hyperskillProject
      val projectShouldBeUpdated = hyperskillProject != null && hyperskillProject.shouldBeUpdated(courseFromServer?.hyperskillProject)
      val projectLessonShouldBeUpdated = courseFromServer != null && projectLesson?.shouldBeUpdated(project, courseFromServer) ?: false
      val problemsUpdates = getProblemsUpdate()

      var isUpdated = false
      if (projectShouldBeUpdated || projectLessonShouldBeUpdated || problemsUpdates.isNotEmpty()) {
        if (HyperskillSettings.INSTANCE.updateAutomatically) {
          doUpdate(courseFromServer, problemsUpdates)
          isUpdated = true
        }
        else {
          showUpdateAvailableNotification(project) {
            runInBackground(project, EduCoreBundle.message("update.process"), false) {
              doUpdate(courseFromServer, problemsUpdates)
            }
          }
        }
      }
      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded
        course.updateDate = Date()
        YamlFormatSynchronizer.saveRemoteInfo(course)
        onFinish(isUpdated)
      }
    }
  }

  private fun HyperskillProject.shouldBeUpdated(hyperskillProject: HyperskillProject?): Boolean {
    return when {
      hyperskillProject != null && title != hyperskillProject.title -> true
      else -> false
    }
  }

  private fun Lesson.getProblemsUpdates(): List<TaskUpdate> {
    val tasksFromServer = HyperskillConnector.getInstance().getProblems(this.course, this, taskList.map { it.id })
    val localTasks = taskList.associateBy { it.id }

    val result = mutableListOf<TaskUpdate>()
    for (serverTask in tasksFromServer) {
      val localTask = localTasks[serverTask.id]
      if (localTask != null) {
        val localTaskIsExpired = serverTask.updateDate.isSignificantlyAfter(localTask.updateDate)
        val serverTaskIsDifferent = taskIsDifferent(localTask, serverTask)
        serverTask.parent = localTask.lesson
        if (localTaskIsExpired || serverTaskIsDifferent) {
          result.add(TaskUpdate(localTask, serverTask))
        }
      }
    }
    return result
  }

  /**
   * Tasks can be different when tasks have different description.
   * It can happen because of bugs
   */
  private fun taskIsDifferent(first: Task, second: Task): Boolean {
    val result = first.descriptionText != second.descriptionText ||
                 first.feedbackLink != second.feedbackLink ||
                 first.name != second.name
    if (result) return true

    return when {
      first is UnsupportedTask && second !is UnsupportedTask -> true
      first is ChoiceTask && second is ChoiceTask -> {
        first.choiceOptions != second.choiceOptions
      }
      first is SortingTask && second is SortingTask -> {
        first.options != second.options
      }
      first is MatchingTask && second is MatchingTask -> {
        (first.options != second.options) || (first.captions != second.captions)
      }
      first is RemoteEduTask && second is RemoteEduTask -> {
        first.checkProfile != second.checkProfile
      }
      else -> false
    }
  }

  class TaskUpdate(val localTask: Task, val taskFromServer: Task)

  @VisibleForTesting
  fun doUpdate(remoteCourse: HyperskillCourse?, problemsUpdates: List<TaskUpdate>) {
    if (remoteCourse != null) {
      updateCourse(remoteCourse)
      updateProjectLesson(remoteCourse)
    }
    updateProblems(problemsUpdates)

    StudyTaskManager.getInstance(project).updateAuthorContentsStorageAndTaskFileContents()

    // update environment at the end as it requires project reload
    if (remoteCourse != null && remoteCourse.environment != course.environment) {
      course.updateDate = Date()
      course.environment = remoteCourse.environment
      YamlFormatSynchronizer.saveItemWithRemoteInfo(course)
      ProjectManager.getInstance().reloadProject(project)
    }
    showUpdateCompletedNotification(project, EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT))
    runInEdt {
      if (project.isDisposed) return@runInEdt
      project.messageBus.syncPublisher(CourseUpdateListener.COURSE_UPDATE).courseUpdated(project, course)
    }
  }

  private fun updateCourse(remoteCourse: HyperskillCourse) {
    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      course.description = remoteCourse.description
      course.hyperskillProject ?: return@invokeAndWaitIfNeeded
      remoteCourse.hyperskillProject ?: return@invokeAndWaitIfNeeded
      course.hyperskillProject!!.title = remoteCourse.hyperskillProject!!.title
      course.hyperskillProject!!.description = remoteCourse.hyperskillProject!!.description
    }
  }

  private fun updateProblems(problemsUpdates: List<TaskUpdate>) {
    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      problemsUpdates.forEach {
        val localTask = it.localTask
        val taskFromServer = it.taskFromServer
        val hasLocalTaskBecomeSupported = localTask is UnsupportedTask && taskFromServer !is UnsupportedTask
        if (hasLocalTaskBecomeSupported) {
          replaceTaskInCourse(localTask as UnsupportedTask, taskFromServer)
        }
        if (localTask.status != CheckStatus.Solved || hasLocalTaskBecomeSupported) {
          // if name of remote task changes name of dir local task will not
          GeneratorUtils.createTaskContent(project, taskFromServer, localTask.getDir(project.courseDir)!!)
        }
        updateTaskDescription(project, localTask, taskFromServer)
        localTask.updateDate = taskFromServer.updateDate
        if (localTask is RemoteEduTask && taskFromServer is RemoteEduTask) {
          localTask.checkProfile = taskFromServer.checkProfile
        }
        YamlFormatSynchronizer.saveItemWithRemoteInfo(localTask)
      }
    }
  }

  private fun replaceTaskInCourse(localTask: UnsupportedTask, taskFromServer: Task) {
    val lesson = localTask.parent
    lesson.removeItem(localTask)
    lesson.addItem(localTask.index - 1, taskFromServer)
    taskFromServer.index = localTask.index
    taskFromServer.name = localTask.name
  }

  private fun updateProjectLesson(remoteCourse: HyperskillCourse) {
    val lesson = course.getProjectLesson() ?: return
    val remoteLesson = remoteCourse.getProjectLesson() ?: return

    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      for ((task, remoteTask) in lesson.taskList.zip(remoteLesson.taskList)) {
        if (!task.updateDate.before(remoteTask.updateDate)) continue

        if (task.status != CheckStatus.Solved) {
          // With current logic of next/prev action for hyperskill tasks
          // update of non-test files makes sense only for first task
          updateFrameworkLessonFiles(project, lesson, task, remoteTask, task.index == 1)
        }

        updateTaskDescription(project, task, remoteTask)
        task.updateDate = remoteTask.updateDate
        YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
      }

      val courseDir = project.courseDir
      for (additionalFile in remoteCourse.additionalFiles) {
        GeneratorUtils.createChildFile(project, courseDir, additionalFile.name, additionalFile.text)
      }
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(HyperskillCourseUpdater::class.java)

    @JvmStatic
    @VisibleForTesting
    fun FrameworkLesson.shouldBeUpdated(project: Project, remoteCourse: HyperskillCourse): Boolean {
      val lessonFromServer = remoteCourse.getProjectLesson() ?: return false
      val tasksFromServer = lessonFromServer.taskList
      val localTasks = taskList
      return when {
        !shouldFrameworkLessonBeUpdated(lessonFromServer) -> false
        localTasks.zip(tasksFromServer).any { (task, remoteTask) -> remoteTask.updateDate.isSignificantlyAfter(task.updateDate) } -> true
        needUpdateCourseAdditionalFiles(project, remoteCourse.additionalFiles) -> true
        else -> false
      }
    }

    private fun needUpdateCourseAdditionalFiles(project: Project, remoteFiles: List<EduFile>): Boolean {
      val courseDir = project.courseDir
      for (remoteFile in remoteFiles) {
        val needToUpdate = invokeAndWaitIfNeeded {
          runWriteAction {
            if (project.isDisposed) return@runWriteAction false
            val file = courseDir.findFileByRelativePath(remoteFile.name) ?: return@runWriteAction true
            val text = try {
              file.loadEncodedContent()
            }
            catch (e: IOException) {
              LOG.warn("Failed to load text of `${remoteFile.name}` additional file", e)
              return@runWriteAction true
            }
            text != remoteFile.text
          }
        }
        if (needToUpdate) return true
      }

      return false
    }
  }
}