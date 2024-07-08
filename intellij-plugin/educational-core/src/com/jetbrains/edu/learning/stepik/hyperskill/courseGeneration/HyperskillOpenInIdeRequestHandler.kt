package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.requestFocus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.KOTLIN
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTaskType
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ALL_FILES
import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequestHandler
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils.navigateToTask
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepSource
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

object HyperskillOpenInIdeRequestHandler : OpenInIdeRequestHandler<HyperskillOpenRequest>() {
  private val LOG = Logger.getInstance(HyperskillOpenInIdeRequestHandler::class.java)
  override val courseLoadingProcessTitle: String get() = EduCoreBundle.message("hyperskill.loading.project")

  override fun openInExistingProject(
    request: HyperskillOpenRequest,
    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?
  ): Boolean {
    val (project, course) = findExistingProject(findProject, request) ?: return false
    val hyperskillCourse = course as HyperskillCourse
    when (request) {
      is HyperskillOpenStepRequestBase -> {
        val stepId = request.stepId
        hyperskillCourse.addProblemsWithTopicWithFiles(project, getStepSource(stepId, request.isLanguageSelectedByUser))
        hyperskillCourse.selectedProblem = stepId
        runInEdt {
          requestFocus()
          navigateToStep(project, hyperskillCourse, stepId)
        }
        synchronizeProjectOnStepOpening(project, hyperskillCourse, stepId)
      }

      is HyperskillOpenProjectStageRequest -> {
        if (hyperskillCourse.getProjectLesson() == null) {
          computeUnderProgress(project, EduCoreBundle.message("hyperskill.loading.stages")) {
            HyperskillConnector.getInstance().loadStages(hyperskillCourse)
          }
          hyperskillCourse.init(false)
          val projectLesson = hyperskillCourse.getProjectLesson() ?: return false
          val courseDir = project.courseDir
          GeneratorUtils.createLesson(project, projectLesson, courseDir)
          GeneratorUtils.unpackAdditionalFiles(CourseInfoHolder.fromCourse(course, courseDir), ALL_FILES)
          YamlFormatSynchronizer.saveAll(project)
          course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.DEPENDENCIES_UPDATED)
          synchronizeProjectOnStageOpening(project, hyperskillCourse, projectLesson.taskList)
        }
        course.selectedStage = request.stageId
        runInEdt { openSelectedStage(hyperskillCourse, project) }
      }

    }
    return true
  }

  private fun navigateToStep(project: Project, course: Course, stepId: Int) {
    if (stepId == 0) {
      return
    }
    val task = getTask(course, stepId) ?: return
    navigateToTask(project, task)
  }

  private fun getTask(course: Course, stepId: Int): Task? {
    val taskRef = Ref<Task>()
    course.visitLessons { lesson: Lesson ->
      val task = lesson.getTaskById(stepId) ?: return@visitLessons
      taskRef.set(task)
    }
    return taskRef.get()
  }

  private fun findExistingProject(
    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?,
    request: HyperskillOpenRequest
  ): Pair<Project, Course>? {
    return when (request) {
      is HyperskillOpenProjectStageRequest -> findProject { it.matchesById(request.projectId) }
      is HyperskillOpenStepWithProjectRequest -> {
        val hyperskillLanguage = request.language
        val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage) ?: return null

        findProject { it.matchesById(request.projectId) && it.languageId == languageId && it.languageVersion == languageVersion }
        ?: findProject { course -> course.isHyperskillProblemsCourse(hyperskillLanguage) }
      }

      is HyperskillOpenStepRequest -> {
        val hyperskillLanguage = request.language
        val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage) ?: return null
        findProject { it is HyperskillCourse && it.languageId == languageId && it.languageVersion == languageVersion }
        ?: findProject { course -> course.isHyperskillProblemsCourse(hyperskillLanguage) }
      }
    }
  }

  private fun Course.isHyperskillProblemsCourse(hyperskillLanguage: String) =
    this is HyperskillCourse && name in listOf(
      getProblemsProjectName(hyperskillLanguage),
      getLegacyProblemsProjectName(hyperskillLanguage)
    )

  private fun Course.matchesById(projectId: Int) = this is HyperskillCourse && hyperskillProject?.id == projectId

  fun createHyperskillCourse(
    request: HyperskillOpenRequest,
    hyperskillLanguage: String,
    hyperskillProject: HyperskillProject
  ): Result<HyperskillCourse, CourseValidationResult> {
    val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage)
                                        ?: return Err(
                                          ValidationErrorMessage(
                                            EduCoreBundle.message(
                                              "hyperskill.unsupported.language",
                                              hyperskillLanguage
                                            )
                                          )
                                        )

    if (!hyperskillProject.useIde) {
      return Err(ValidationErrorMessageWithHyperlinks(EduCoreBundle.message("hyperskill.project.not.supported", HYPERSKILL_PROJECTS_URL)))
    }

    val eduEnvironment = hyperskillProject.eduEnvironment
                         ?: return Err(ValidationErrorMessage("Unsupported environment ${hyperskillProject.environment}"))

    if (request is HyperskillOpenStepWithProjectRequest) {
      // These condition is about opening e.g. Python problem with chosen Kotlin's project,
      // otherwise - open Kotlin problem in current Kotlin project itself later below
      if (hyperskillLanguage != hyperskillProject.language) {
        return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))
      }

      // This is about opening Kotlin problem with currently chosen Android's project
      // But all Android projects are always Kotlin one's
      // So it should be possible to open problem in IntelliJ IDEA too e.g. (EDU-4641)
      if (eduEnvironment == EduNames.ANDROID && hyperskillLanguage == KOTLIN) {
        return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))
      }
    }
    if (request is HyperskillOpenStepRequest) return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))

    // Android projects must be opened in Android Studio only
    if (eduEnvironment == EduNames.ANDROID && !EduUtilsKt.isAndroidStudio()) {
      return Err(ValidationErrorMessageWithHyperlinks(EduCoreBundle.message("rest.service.android.not.supported")))
    }

    return Ok(HyperskillCourse(hyperskillProject, languageId, languageVersion, eduEnvironment))
  }

  override fun getCourse(request: HyperskillOpenRequest, indicator: ProgressIndicator): Result<Course, CourseValidationResult> {
    if (request is HyperskillOpenStepRequest) {
      val newProject = HyperskillProject()
      val hyperskillLanguage = request.language
      val hyperskillCourse = createHyperskillCourse(request, hyperskillLanguage, newProject).onError { return Err(it) }
      hyperskillCourse.addProblemsWithTopicWithFiles(null, getStepSource(request.stepId, request.isLanguageSelectedByUser))
      hyperskillCourse.selectedProblem = request.stepId
      return Ok(hyperskillCourse)
    }
    request as HyperskillOpenWithProjectRequestBase
    val hyperskillProject = HyperskillConnector.getInstance().getProject(request.projectId).onError {
      return Err(ValidationErrorMessage(it))
    }

    val hyperskillLanguage = if (request is HyperskillOpenStepWithProjectRequest) request.language else hyperskillProject.language

    val hyperskillCourse = createHyperskillCourse(request, hyperskillLanguage, hyperskillProject).onError {
      return Err(it)
    }

    hyperskillCourse.validateLanguage(hyperskillLanguage).onError { return Err(it) }

    when (request) {
      is HyperskillOpenStepWithProjectRequest -> {
        hyperskillCourse.addProblemsWithTopicWithFiles(null, getStepSource(request.stepId, request.isLanguageSelectedByUser))
        hyperskillCourse.selectedProblem = request.stepId
      }

      is HyperskillOpenProjectStageRequest -> {
        indicator.text2 = EduCoreBundle.message("hyperskill.loading.stages")
        HyperskillConnector.getInstance().loadStages(hyperskillCourse)
        hyperskillCourse.selectedStage = request.stageId
      }
    }
    return Ok(hyperskillCourse)
  }

  @VisibleForTesting
  fun getStepSource(stepId: Int, isLanguageSelectedByUser: Boolean): HyperskillStepSource {
    val connector = HyperskillConnector.getInstance()
    val stepSource = connector.getStepSource(stepId).onError { error(it) }

    // Choosing language by user is allowed only for Data tasks, see EDU-4718
    if (isLanguageSelectedByUser && !stepSource.isDataTask()) {
      error("Language has been selected by user not for data task, but it must be specified for other tasks in request")
    }
    return stepSource
  }

  private fun HyperskillStepSource.isDataTask(): Boolean = block?.name == DataTask.DATA_TASK_TYPE

  private fun Lesson.addProblems(stepSources: List<HyperskillStepSource>): Result<List<Task>, String> {
    val existingTasksIds = items.map { it.id }
    val stepsSourceForAdding = stepSources.filter { "" + it.id !in existingTasksIds }

    val tasks = HyperskillConnector.getTasks(course, this, stepsSourceForAdding)
    tasks.forEach(this::addTask)
    return Ok(tasks)
  }

  private fun HyperskillCourse.createTopicsSection(): Section {
    val section = Section()
    section.name = HYPERSKILL_TOPICS
    section.index = items.size + 1
    section.parent = this
    addSection(section)
    return section
  }

  private fun Section.createTopicLesson(name: String): Lesson {
    val lesson = Lesson()
    lesson.name = name
    lesson.index = this.items.size + 1
    lesson.parent = this
    addLesson(lesson)
    return lesson
  }

  private fun HyperskillStepSource.getTopicWithRecommendedSteps(): Result<Pair<String, List<HyperskillStepSource>>, String> {
    val connector = HyperskillConnector.getInstance()
    val topicId = topic ?: return Err("Topic must not be null")

    val stepSources = connector.getStepsForTopic(topicId)
      .onError { return Err(it) }
      .filter { it.isRecommended || it.id == id }.toMutableList()

    val theoryTask = stepSources.find { it.block?.name == HyperskillTaskType.TEXT.type }
    if (theoryTask != null) {
      stepSources.remove(theoryTask)
      stepSources.add(0, theoryTask)
    }

    val theoryTitle = stepSources.find { it.block?.name == HyperskillTaskType.TEXT.type }?.title
    if (theoryTitle != null) {
      return Ok(Pair(theoryTitle, stepSources))
    }

    LOG.warn("Can't get theory step title for ${id} step")
    val problemTitle = title
    return Ok(Pair(problemTitle, stepSources))
  }

  @VisibleForTesting
  fun HyperskillCourse.addProblemsWithTopicWithFiles(project: Project?, stepSource: HyperskillStepSource): Result<Unit, String> {
    return computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.problems")) {
      var localTopicsSection = getTopicsSection()
      val createSectionDir = localTopicsSection == null
      if (localTopicsSection == null) {
        localTopicsSection = createTopicsSection()
      }

      val (topicNameSource, stepSources) = stepSource.getTopicWithRecommendedSteps().onError { return@computeUnderProgress Err(it) }
      var localTopicLesson = localTopicsSection.getLesson { it.presentableName == topicNameSource }
      val createLessonDir = localTopicLesson == null
      if (localTopicLesson == null) {
        localTopicLesson = localTopicsSection.createTopicLesson(topicNameSource)
      }

      val tasks = localTopicLesson.addProblems(stepSources).onError { return@computeUnderProgress Err(it) }
      localTopicsSection.init(this, false)

      if (project != null) {
        when {
          createSectionDir -> saveSectionDir(project, course, localTopicsSection, localTopicLesson, tasks)
          createLessonDir -> saveLessonDir(project, localTopicsSection, localTopicLesson, tasks)
          else -> saveTasks(project, localTopicLesson, tasks)
        }

        if (tasks.isNotEmpty()) {
          course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
        }
      }
      Ok(Unit)
    }
  }

  private fun saveSectionDir(
    project: Project,
    course: Course,
    topicsSection: Section,
    topicLesson: Lesson,
    tasks: List<Task>
  ) {
    GeneratorUtils.createSection(project, topicsSection, project.courseDir)
    tasks.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) }
    YamlFormatSynchronizer.saveItem(topicLesson)
    YamlFormatSynchronizer.saveItem(topicsSection)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun saveLessonDir(
    project: Project,
    topicSection: Section,
    topicLesson: Lesson,
    tasks: List<Task>
  ) {
    val parentDir = topicSection.getDir(project.courseDir) ?: error("Can't get directory of Topics section")
    GeneratorUtils.createLesson(project, topicLesson, parentDir)
    tasks.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) }
    YamlFormatSynchronizer.saveItem(topicLesson)
    YamlFormatSynchronizer.saveItem(topicSection)
  }

  private fun saveTasks(
    project: Project,
    topicLesson: Lesson,
    tasks: List<Task>,
  ) {
    tasks.forEach { task ->
      topicLesson.getDir(project.courseDir)?.let { lessonDir ->
        GeneratorUtils.createTask(project, task, lessonDir)
        YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
      }
    }
    YamlFormatSynchronizer.saveItem(topicLesson)
  }

  private fun synchronizeProjectOnStepOpening(project: Project, course: HyperskillCourse, stepId: Int) {
    if (isUnitTestMode) {
      return
    }

    val task = course.getProblem(stepId) ?: return
    val tasks = task.lesson.taskList
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
  }

  private fun synchronizeProjectOnStageOpening(project: Project, course: HyperskillCourse, tasks: List<Task>) {
    if (isUnitTestMode) {
      return
    }
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
    HyperskillStartupActivity.synchronizeTopics(project, course)
  }
}