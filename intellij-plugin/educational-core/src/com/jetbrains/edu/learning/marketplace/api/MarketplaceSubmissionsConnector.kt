package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBAccountInfoService
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.agreement.UserAgreementDialogResultState
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderDependencyMixin
import com.jetbrains.edu.learning.json.mixins.AnswerPlaceholderWithAnswerMixin
import com.jetbrains.edu.learning.json.mixins.EduTestInfoMixin
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showFailedToDeleteNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showNoSubmissionsToDeleteNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showSubmissionsDeletedSuccessfullyNotification
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.network.executeCall
import com.jetbrains.edu.learning.network.executeParsingErrors
import com.jetbrains.edu.learning.submissions.*
import okhttp3.ConnectionPool
import okhttp3.ResponseBody
import org.jetbrains.annotations.VisibleForTesting
import retrofit2.Call
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.BufferedInputStream
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.URL

@Service(Service.Level.APP)
class MarketplaceSubmissionsConnector {
  private val connectionPool: ConnectionPool = ConnectionPool()
  private val converterFactory: JacksonConverterFactory
  val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
    objectMapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
    objectMapper.addMixIn(EduTestInfo::class.java, EduTestInfoMixin::class.java)
    objectMapper
  }

  private val submissionsServiceUrl: String
    get() = SubmissionsServiceHost.getSelectedUrl()

  init {
    converterFactory = JacksonConverterFactory.create(objectMapper)
  }

  private val submissionsService: SubmissionsService
    get() = submissionsService()

  private fun submissionsService(): SubmissionsService {
    val uidToken = if (RemoteEnvHelper.isRemoteDevServer() && !isUnitTestMode) {
      RemoteEnvHelper.getUserUidToken() ?: error("User UID was not found, it might require more time to retrieve it")
    }
    else {
      JBAccountInfoService.getInstance()?.userData?.id ?: error("Nullable JB account ID token in user data")
    }

    val retrofit = createRetrofitBuilder(submissionsServiceUrl, connectionPool, "u.$uidToken")
      .addConverterFactory(converterFactory)
      .build()

    return retrofit.create(SubmissionsService::class.java)
  }

  fun deleteAllSubmissions(project: Project?, courseId: Int? = null, loginName: String?): Boolean {
    LOG.info("Deleting submissions ${logLoginName(loginName)} ${logCourseId(courseId)}")
    val deleteCall: Call<ResponseBody> = if (courseId != null) {
      submissionsService.deleteAllSubmissions(courseId)
    }
    else {
      submissionsService.deleteAllSubmissions()
    }

    val response = deleteCall.executeCall().onError {
      LOG.error("Failed to delete all submissions ${logLoginName(loginName)} ${logCourseId(courseId)}. Error message: $it")
      showFailedToDeleteNotification(project, courseId, loginName)
      return false
    }
    logAndNotifyAfterDeletionAttempt(response, project, courseId, loginName)

    return response.code() == HTTP_NO_CONTENT
  }

  private fun logLoginName(loginName: String?): String = if (loginName != null) "for user $loginName" else ""

  private fun logCourseId(courseId: Int?): String = if (courseId != null) "for course $courseId" else ""

  // TODO: generalize fetching all pages for paginated requests in common method EDU-7110
  fun getAllSubmissions(courseId: Int): List<MarketplaceSubmission> {
    var currentPage = 1
    val allSubmissions = mutableListOf<MarketplaceSubmission>()
    do {
      val submissionsList = submissionsService.getAllSubmissionsForCourse(courseId, currentPage).executeParsingErrors().onError {
        LOG.warn("Failed to get all submissions for course $courseId. Error message: $it")
        return emptyList()
      }.body() ?: break
      val submissions = submissionsList.submissions
      allSubmissions.addAll(submissions)
      currentPage += 1
    }
    while (submissions.isNotEmpty() && submissionsList.hasNext)
    return allSubmissions
  }

  // TODO: generalize fetching all pages for paginated requests EDU-7110
  fun getCourseStateOnClose(courseId: Int): List<MarketplaceStateOnClose> {
    var currentPage = 1
    val allStates = mutableListOf<MarketplaceStateOnClose>()
    do {
      val page = submissionsService.getStateOnClose(courseId, currentPage).executeParsingErrors().onError {
        LOG.warn("Failed to get all states on close for course $courseId. Error message: $it")
        return emptyList()
      }.body() ?: break
      allStates.addAll(page.states)
      currentPage += 1
    }
    while (page.states.isNotEmpty() && page.hasNext)
    return allStates
  }

  // TODO: generalize fetching all pages for paginated requests EDU-7110
  /**
   * Fetches only N shared solutions for each task on the course that user has solved
   */
  fun getSharedSolutionsForCourse(courseId: Int, updateVersion: Int): List<MarketplaceSubmission> {
    LOG.info("Loading all published solutions for course with courseId = $courseId, updateVersion = $updateVersion")

    val courseSharedSolutions = mutableListOf<MarketplaceSubmission>()
    var currentPage = 1
    do {
      val submissionsList = submissionsService.getAllPublicSubmissionsForCourse(
        courseId, updateVersion, currentPage
      ).executeParsingErrors().onError {
        LOG.warn("Failed to get all shared submissions for course $courseId. Error message: $it")
        return emptyList()
      }.body() ?: break
      val sharedSolutions = submissionsList.submissions
      courseSharedSolutions.addAll(sharedSolutions)
      currentPage += 1
    }
    while (sharedSolutions.isNotEmpty() && submissionsList.hasNext)

    return courseSharedSolutions
  }

  fun getSharedSubmissionsForTask(course: Course, taskId: Int): TaskCommunitySubmissions? {
    val (courseId, updateVersion) = course.id to course.marketplaceCourseVersion
    LOG.info("Loading shared solutions for task $taskId in course with courseId = $courseId, updateVersion = $updateVersion")

    val responseBody = submissionsService.getSharedSubmissionsForTask(
      courseId, updateVersion, taskId
    ).executeParsingErrors().onError {
      LOG.warn("Failed to get shared submissions for task $taskId. Error message: $it")
      return null
    }.body() ?: return null

    val sharedSubmissions = responseBody.submissions.takeIf { it.isNotEmpty() }
    return sharedSubmissions?.let { TaskCommunitySubmissions(it.toMutableList(), responseBody.hasNext) }
  }

  fun getMoreSharedSubmissions(course: Course, taskId: Int, latest: Int, oldest: Int): TaskCommunitySubmissions? {
    val (courseId, updateVersion) = course.id to course.marketplaceCourseVersion
    LOG.info("Loading more shared solutions for task $taskId in course with courseId = $courseId, updateVersion = $updateVersion")

    val responseBody = submissionsService.getMoreSharedSubmissionsForTask(
      courseId, updateVersion, taskId, latest, oldest
    ).executeParsingErrors().onError {
      LOG.warn("Failed to get more shared submissions for task $taskId. Error message: $it")
      return null
    }.body() ?: return null

    val sharedSubmissions = responseBody.submissions.takeIf { it.isNotEmpty() }
    return sharedSubmissions?.let { TaskCommunitySubmissions(it.toMutableList(), responseBody.hasNext) }
  }

  fun markTheoryTaskAsCompleted(task: TheoryTask) {
    val emptySubmission = MarketplaceSubmission(
      task.id,
      CheckStatus.Solved,
      "",
      null,
      task.course.marketplaceCourseVersion
    )
    LOG.info("Marking theory task ${task.name} as completed")
    doPostSubmission(task.course.id, task.id, emptySubmission)
  }

  @RequiresBackgroundThread
  fun loadSolutionFiles(solutionKey: String): List<SolutionFile> {

    val solutionsDownloadLink = submissionsService.getSolutionDownloadLink(solutionKey).executeParsingErrors().onError {
      LOG.warn("Failed to obtain a download link for solution key $solutionKey: $it")
      return emptyList()
    }.body()?.string() ?: error("Nullable solutionsDownloadLink")

    val solutions: String = loadSolutionByLink(solutionsDownloadLink)

    return objectMapper.readValue(solutions, object : TypeReference<List<SolutionFile>>() {})
           ?: error("Failed to load solution files for solution key $solutionKey")
  }

  fun postSubmission(project: Project, task: Task, result: CheckResult): MarketplaceSubmission {
    val course = task.course

    val submission = createSubmission(project, task, course, result.executedTestsInfo)

    val postedSubmission = doPostSubmission(course.id, task.id, submission).onError { error("failed to post submission") }
    submission.id = postedSubmission.id
    submission.time = postedSubmission.time
    return submission
  }

  private fun createSubmission(
    project: Project,
    task: Task,
    course: Course,
    testInfo: List<EduTestInfo> = emptyList()
  ): MarketplaceSubmission {
    val solutionFiles = solutionFilesList(project, task).filter { it.isVisible }
    val solutionText = objectMapper.writeValueAsString(solutionFiles).trimIndent()

    return MarketplaceSubmission(
      task.id,
      task.status,
      solutionText,
      solutionFiles,
      course.marketplaceCourseVersion,
      testInfo
    )
  }

  private fun createStateOnClose(
    project: Project,
    task: Task,
  ): MarketplaceStateOnClosePost {
    val solutionFiles = solutionFilesList(project, task).filter { it.isVisible }
    val solutionText = objectMapper.writeValueAsString(solutionFiles).trimIndent()

    return MarketplaceStateOnClosePost(
      task.id,
      solutionText
    )
  }

  suspend fun changeSharingPreference(state: Boolean): Result<Response<Unit>, String> {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    LOG.info("Changing solution sharing to state $state for user $loginName")
    val newSharingPreference = if (state) SolutionSharingPreference.ALWAYS else SolutionSharingPreference.NEVER
    return try {
      Ok(submissionsService.changeSharingPreference(newSharingPreference.name))
    }
    catch (e: Exception) {
      LOG.info("Error occurred while changing solution sharing to state $state for user $loginName", e)
      Err(e.message ?: "Something went wrong")
    }
  }

  suspend fun getSharingPreference(): SolutionSharingPreference? {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    LOG.info("Getting solution sharing preference for use $loginName")
    val responseString = try {
      submissionsService.getSharingPreference().string()
    }
    catch (e: Exception) {
      LOG.info("Error occurred while getting solution sharing preference for user $loginName", e)
      null
    }

    return responseString?.let { SolutionSharingPreference.valueOf(it) }
  }

  fun reportSolution(submissionId: Int): Boolean {
    LOG.info("Reporting solution with id $submissionId")
    submissionsService.reportSolution(submissionId).executeParsingErrors().onError {
      LOG.info("Failed to report solution with id $submissionId")
      return false
    }

    return true
  }

  private fun doPostSubmission(courseId: Int, taskId: Int, submission: MarketplaceSubmission): Result<MarketplaceSubmission, String> {
    LOG.info("Posting submission for task $taskId")
    val response = submissionsService.postSubmission(courseId, submission.courseVersion, taskId, submission).executeParsingErrors().onError {
      LOG.warn("Failed to post submission for task $taskId")
      return Err(it)
    }.body() ?: return Err(EduCoreBundle.message("error.failed.to.post.solution"))
    return Ok(response)
  }

  private fun solutionFilesList(project: Project, task: Task): List<SolutionFile> {
    val files = mutableListOf<SolutionFile>()
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find task directory ${task.name}")

    for (taskFile in task.taskFiles.values) {
      val virtualFile = findTaskFileInDirWithSizeCheck(taskFile, taskDir) ?: continue

      runReadAction {
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
        files.add(SolutionFile(taskFile.name, document.text, taskFile.isVisible, taskFile.answerPlaceholders))
      }
    }

    return files.checkNotEmpty()
  }

  @RequiresBackgroundThread
  fun saveCurrentState(project: Project, course: Course) {
    if (!course.isStudy || course !is EduCourse || !course.isMarketplaceRemote) return

    val currentStates = measureTimeAndLog("Collecting the current course state") {
      collectCurrentState(project, course)
    }
    measureTimeAndLog("Posting the current course state") {
      postCurrentState(course, currentStates)
    }
  }

  private fun collectCurrentState(project: Project, course: Course): List<MarketplaceStateOnClosePost> {
    return course.allTasks
      .filter { task -> (task.supportSubmissions || task is TheoryTask) && task.hasChangedFiles(project) }
      .map { createStateOnClose(project, it) }
  }

  private fun Task.hasChangedFiles(project: Project): Boolean {
    for (taskFile in taskFiles.values) {
      if (!taskFile.isVisible) continue
      val document = taskFile.getDocument(project) ?: continue
      if (document.text != taskFile.contents.textualRepresentation) return true
    }
    return false
  }

  private fun postCurrentState(course: EduCourse, currentStates: List<MarketplaceStateOnClosePost>) {
    if (currentStates.isEmpty()) return

    var attempts = 0
    do {
      val remainingStates = doPostChunked(currentStates, course)
      attempts++
    } while (remainingStates.isNotEmpty() && attempts < 4)
  }

  private fun doPostChunked(currentStates: List<MarketplaceStateOnClosePost>, course: EduCourse): List<MarketplaceStateOnClosePost> {
    val remainingStates = mutableListOf<MarketplaceStateOnClosePost>()
    currentStates.chunked(STATES_PER_REQUEST).forEach { stateChunk ->
      when (val res = submissionsService.postStateOnClose(course.id, course.marketplaceCourseVersion, stateChunk).executeParsingErrors()) {
        is Ok -> {
          LOG.info("Successfully sent ${stateChunk.size} states")
        }
        is Err -> {
          LOG.info("Failed to send states with error `${res.error}`")
          remainingStates.addAll(stateChunk)
        }
      }
    }
    return remainingStates
  }

  private fun logAndNotifyAfterDeletionAttempt(response: Response<ResponseBody>, project: Project?, courseId: Int?,loginName: String?) {
    when (response.code()) {
      HTTP_NO_CONTENT -> {
        LOG.info("Successfully deleted all submissions ${logLoginName(loginName)} ${logCourseId(courseId)}")
        showSubmissionsDeletedSuccessfullyNotification(project, courseId, loginName)
      }

      HTTP_NOT_FOUND -> {
        LOG.info("There are no submissions to delete ${logLoginName(loginName)} ${logCourseId(courseId)}")
        showNoSubmissionsToDeleteNotification(project, courseId, loginName)
      }

      else -> {
        val errorMsg = response.errorBody()?.string() ?: "Unknown error"
        LOG.error("Failed to delete all submissions ${logLoginName(loginName)} ${logCourseId(courseId)}. Error message: $errorMsg")
        showFailedToDeleteNotification(project, courseId, loginName)
      }
    }
  }

  @RequiresBackgroundThread
  fun getUserAgreementState(): UserAgreementState? {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName ?: run {
      LOG.info("Unable to get user agreement state for not logged in user")
      return null
    }
    LOG.info("Getting user agreement state for user $loginName")
    val response = submissionsService.getUserAgreementState().executeParsingErrors().onError {
      LOG.info("Error occurred while getting user agreement state for user $loginName")
      return null
    }

    return response.body()?.string()?.let { UserAgreementState.valueOf(it) }
  }

  suspend fun changeUserAgreementAndStatisticsState(result: UserAgreementDialogResultState) {
    changeUserAgreementState(result.agreementState)
    changeUserStatisticsAllowedState(result.isStatisticsSharingAllowed)
  }

  suspend fun changeUserAgreementState(newState: UserAgreementState): Result<Unit, String> {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    val newStateName = newState.name
    LOG.info("Changing User Agreement state to $newStateName for user $loginName")
    return try {
      val response = submissionsService.changeUserAgreementState(newStateName)
      if (response.isSuccessful) {
        Ok(Unit)
      }
      else {
        Err("Failed to change User Agreement state: ${response.errorBody()}. Response code: ${response.code()}")
      }
    }
    catch (e: Exception) {
      LOG.error("Error occurred while changing User Agreement state to $newStateName for user $loginName", e)
      Err(e.message ?: "Failed to update User Agreement state")
    }
  }

  suspend fun changeUserStatisticsAllowedState(newState: Boolean): Result<Unit, String> {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    LOG.info("Changing User Statistics Allowed state to $newState for user $loginName")
    return try {
      val response = submissionsService.changeUserStatisticsAllowedState(newState)
      if (response.isSuccessful) {
        Ok(Unit)
      }
      else {
        Err("Failed to change User Statistics Allowance state: ${response.errorBody()}. Response code: ${response.code()}")
      }
    }
    catch (e: Exception) {
      LOG.error("Error occurred while changing User Statistics Allowed state to $newState for user $loginName", e)
      Err(e.message ?: "Failed to change User Statistics Allowed state")
    }
  }

  suspend fun getUserStatisticsAllowedState(): Boolean? {
    val loginName = JBAccountInfoService.getInstance()?.userData?.loginName
    val response = submissionsService.getUserStatisticsAllowedState()
    val responseBody = response.body()
    if (responseBody == null) {
      LOG.info("Error occurred while getting User Agreement state for user $loginName: ${response.errorBody()}. Response code: ${response.code()}")
    }
    return responseBody
  }

  companion object {
    private val LOG = logger<MarketplaceConnector>()
    @VisibleForTesting
    const val STATES_PER_REQUEST: Int = 100

    @VisibleForTesting
    fun loadSolutionByLink(solutionsDownloadLink: String): String {
      return URL(solutionsDownloadLink).openConnection().getInputStream().use {
        val inputStream = BufferedInputStream(it)
        String(inputStream.readAllBytes())
      }
    }

    fun getInstance(): MarketplaceSubmissionsConnector = service()
  }
}