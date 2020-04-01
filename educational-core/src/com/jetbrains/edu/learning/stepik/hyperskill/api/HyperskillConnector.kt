package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillTaskBuilder
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import okhttp3.ConnectionPool
import retrofit2.Call
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.*

abstract class HyperskillConnector {

  private var authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()

  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper

  init {
    val module = SimpleModule()
    module.addDeserializer(PyCharmStepOptions::class.java, JacksonStepOptionsDeserializer())
    objectMapper = StepikConnector.createMapper(module)
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  protected abstract val baseUrl: String

  private val authorizationService: HyperskillService
    get() {
      val retrofit = createRetrofitBuilder(baseUrl, connectionPool)
        .addConverterFactory(converterFactory)
        .build()

      return retrofit.create(HyperskillService::class.java)
    }

  private val service: HyperskillService
    get() = service(HyperskillSettings.INSTANCE.account)

  private fun service(account: HyperskillAccount?): HyperskillService {
    if (!isUnitTestMode && account != null && !account.tokenInfo.isUpToDate()) {
      account.refreshTokens()
    }

    val retrofit = createRetrofitBuilder(baseUrl, connectionPool, accessToken = account?.tokenInfo?.accessToken)
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(HyperskillService::class.java)
  }

  // Authorization requests:

  fun doAuthorize(vararg postLoginActions: Runnable) {
    createAuthorizationListener(*postLoginActions)
    BrowserUtil.browse(AUTHORISATION_CODE_URL)
  }

  fun login(code: String): Boolean {
    val response = authorizationService.getTokens(CLIENT_ID, CLIENT_SECRET, REDIRECT_URI, code, "authorization_code").executeHandlingExceptions()
    val tokenInfo = response?.body() ?: return false
    val account = HyperskillAccount()
    account.tokenInfo = tokenInfo
    val currentUser = getCurrentUser(account) ?: return false
    account.userInfo = currentUser
    HyperskillSettings.INSTANCE.account = account
    ApplicationManager.getApplication().messageBus.syncPublisher(AUTHORIZATION_TOPIC).userLoggedIn()
    return true
  }

  private fun HyperskillAccount.refreshTokens() {
    val refreshToken = tokenInfo.refreshToken
    val response = authorizationService.refreshTokens("refresh_token", CLIENT_ID, CLIENT_SECRET, refreshToken).executeHandlingExceptions()
    val tokens = response?.body()
    if (tokens != null) {
      updateTokens(tokens)
    }
  }

  // Get requests:

  fun getCurrentUser(account: HyperskillAccount): HyperskillUserInfo? {
    val response = service(account).getCurrentUserInfo().executeHandlingExceptions()
    return response?.body()?.profiles?.firstOrNull()
  }

  fun getStages(projectId: Int): List<HyperskillStage>? {
    val response = service.stages(projectId).executeHandlingExceptions()
    return response?.body()?.stages
  }

  fun getProject(projectId: Int): Result<HyperskillProject, String> {
    return service.project(projectId).executeParsingErrors(true).flatMap {
      val result = it.body()?.projects?.firstOrNull()
      if (result == null) Err(it.message()) else Ok(result)
    }
  }

  private fun getStepSources(stepIds: List<Int>): List<HyperskillStepSource>? {
    val response = service.steps(stepIds.joinToString(separator = ",")).executeHandlingExceptions()
    return response?.body()?.steps
  }

  fun getStepSource(stepId: Int): HyperskillStepSource? {
    val response = service.steps(stepId.toString()).executeHandlingExceptions()
    return response?.body()?.steps?.firstOrNull()
  }

  fun fillTopics(course: HyperskillCourse, project: Project) {
    for ((taskIndex, stage) in course.stages.withIndex()) {
      val topics = getAllTopics(stage)
      if (topics.isEmpty()) continue

      course.taskToTopics[taskIndex] = topics
      runInEdt {
        if (project.isDisposed) return@runInEdt
        TaskDescriptionView.getInstance(project).updateAdditionalTaskTab()
      }
    }
  }

  private fun getAllTopics(stage: HyperskillStage): List<HyperskillTopic> {
    var page = 1
    val topics = mutableListOf<HyperskillTopic>()
    do {
      val topicsList = service.topics(stage.id, page).executeHandlingExceptions(true)?.body() ?: break
      topics.addAll(topicsList.topics.filter { it.theoryId != null })
      page += 1
    }
    while (topicsList.topics.isNotEmpty() && topicsList.meta["has_next"] == true)
    return topics
  }

  fun getLesson(course: HyperskillCourse, attachmentLink: String): Lesson {
    val progressIndicator = ProgressManager.getInstance().progressIndicator

    val lesson = FrameworkLesson()
    lesson.index = 1
    lesson.course = course
    progressIndicator?.checkCanceled()
    val stepSources = getStepSources(course.stages.map { it.stepId }) ?: emptyList()

    progressIndicator?.checkCanceled()
    val tasks = getTasks(course, lesson, stepSources)
    for (task in tasks) {
      lesson.addTask(task)
    }
    loadAndFillAdditionalCourseInfo(course, attachmentLink)
    loadAndFillLessonAdditionalInfo(lesson)
    return lesson
  }

  fun getTasks(course: Course, lesson: Lesson, stepSources: List<HyperskillStepSource>): List<Task> {
    val tasks = ArrayList<Task>()
    for (step in stepSources) {
      val builder = HyperskillTaskBuilder(course, lesson, step, step.id)
      if (!builder.isSupported(step.block!!.name)) continue
      val task = builder.createTask(step.block!!.name)
      if (task != null) {
        tasks.add(task)
      }
    }
    return tasks
  }

  fun getCodeChallenges(course: Course, lesson: Lesson, steps: List<Int>): List<Task> {
    val stepSources = getStepSources(steps) ?: return emptyList()
    return getTasks(course, lesson, stepSources)
  }

  fun loadStages(hyperskillCourse: HyperskillCourse): Boolean {
    val hyperskillProject = hyperskillCourse.hyperskillProject ?: error("No Hyperskill project")
    val projectId = hyperskillProject.id
    if (hyperskillCourse.stages.isEmpty()) {
      val stages = getStages(projectId) ?: return false
      hyperskillCourse.stages = stages
    }
    val stages = hyperskillCourse.stages
    val lesson = getLesson(hyperskillCourse, hyperskillProject.ideFiles)
    if (lesson.taskList.size != stages.size) {
      LOG.warn("Course has ${stages.size} stages, but ${lesson.taskList.size} tasks")
      return false
    }

    lesson.taskList.forEachIndexed { index, task ->
      task.feedbackLink = feedbackLink(projectId, stages[index])
      task.name = stages[index].title
    }
    lesson.name = hyperskillCourse.name

    // We want project lesson to be the first
    // It's possible to open Problems in IDE without loading project lesson (stages)
    // So we need to update indices of existing Problems in this case
    if (hyperskillCourse.lessons.isNotEmpty()) {
      for (existingLesson in hyperskillCourse.lessons) {
        existingLesson.index += 1
      }
    }
    hyperskillCourse.addLesson(lesson)
    hyperskillCourse.sortItems()
    return true
  }

  fun feedbackLink(project: Int, stage: HyperskillStage): FeedbackLink {
    return FeedbackLink("$HYPERSKILL_PROJECTS_URL/$project/stages/${stage.id}/implement")
  }

  fun getSubmission(stepId: Int, page: Int = 1): Submission? {
    val userId = HyperskillSettings.INSTANCE.account?.userInfo?.id ?: error("Attempt to get submission for non authorized user")
    return service.submission(userId, stepId, page).executeHandlingExceptions()?.body()?.submissions?.firstOrNull()
  }

  fun getSubmissionById(submissionId: Int): Result<Submission, String> {
    return withTokenRefreshIfNeeded { service.submission(submissionId).executeAndExtractFirst(SubmissionsList::submissions) }
  }

  // Post requests:

  fun postSubmission(submission: Submission): Result<Submission, String> {
    return withTokenRefreshIfNeeded { service.submission(submission).executeAndExtractFirst(SubmissionsList::submissions) }
  }

  fun postAttempt(step: Int): Result<Attempt, String> {
    return withTokenRefreshIfNeeded { service.attempt(Attempt(step)).executeAndExtractFirst(AttemptsList::attempts) }
  }

  private fun <T, R> Call<T>.executeAndExtractFirst(extractResult: T.() -> List<R>): Result<R, String> {
    return executeParsingErrors(true).flatMap {
      val result = it.body()?.extractResult()?.firstOrNull()
      if (result == null) Err(EduCoreBundle.message("error.failed.to.post.solution", EduNames.JBA)) else Ok(result)
    }
  }

  private fun <T> withTokenRefreshIfNeeded(call: () -> Result<T, String>): Result<T, String> {
    val result = call()
    if (!isUnitTestMode && !ApplicationManager.getApplication().isInternal
        && result is Err && result.error == EduCoreBundle.message("error.forbidden")) {
      HyperskillSettings.INSTANCE.account?.refreshTokens()
      return call()
    }
    return result
  }

  private fun createAuthorizationListener(vararg postLoginActions: Runnable) {
    authorizationBusConnection.disconnect()
    authorizationBusConnection = ApplicationManager.getApplication().messageBus.connect()
    authorizationBusConnection.subscribe(AUTHORIZATION_TOPIC, object : EduLogInListener {
      override fun userLoggedOut() {}

      override fun userLoggedIn() {
        for (action in postLoginActions) {
          action.run()
        }
      }
    })
  }

  companion object {
    private val LOG = Logger.getInstance(HyperskillConnector::class.java)

    @JvmStatic
    val AUTHORIZATION_TOPIC = com.intellij.util.messages.Topic.create("Edu.hyperskillLoggedIn", EduLogInListener::class.java)

    @JvmStatic
    fun getInstance(): HyperskillConnector = service()
  }

}
