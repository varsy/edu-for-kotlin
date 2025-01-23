package com.jetbrains.edu.learning.submissions

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.createTopic
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.api.MarketplaceSubmission
import com.jetbrains.edu.learning.submissions.provider.CommunitySubmissionsProvider
import com.jetbrains.edu.learning.submissions.provider.CommunitySubmissionsProvider.Companion.getCommunitySubmissionsProvider
import com.jetbrains.edu.learning.submissions.provider.SubmissionsProvider
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores and returns submissions for courses with submissions support if they are already loaded or delegates loading
 * to SubmissionsProvider.
 *
 * @see com.jetbrains.edu.learning.submissions.provider.SubmissionsProvider
 */
@Service(Service.Level.PROJECT)
class SubmissionsManager(private val project: Project) : EduTestAware {

  private val submissions = ConcurrentHashMap<Int, List<Submission>>()

  private val communitySubmissions = ConcurrentHashMap<Int, TaskCommunitySubmissions>()

  // Guarded by synchronized `getCourseStateOnClose` method
  private var courseStateOnClose: Map<Int, Submission> = emptyMap()

  var course: Course? = project.course
    @TestOnly set

  fun getSubmissionsFromMemory(taskIds: Set<Int>): List<Submission> {
    return taskIds.mapNotNull { submissions[it] }.flatten().sortedByDescending { it.time }
  }

  fun getCommunitySubmissionsFromMemory(taskId: Int): List<Submission>? {
    return communitySubmissions[taskId]?.submissions?.sortedByDescending { it.time }
  }

  private fun getCommunitySubmissionFromMemory(taskId: Int, submissionId: Int): Submission? {
    return communitySubmissions[taskId]?.submissions?.find { it.id == submissionId }
  }

  fun getOrLoadSubmissions(tasks: List<Task>): List<Submission> {
    val taskIds = tasks.map { it.id }.toSet()
    val submissionsFromMemory = getSubmissionsFromMemory(taskIds)
    return submissionsFromMemory.ifEmpty {
      val course = course ?: error("Nullable Course")
      val submissionsProvider =
        SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: error("SubmissionProvider for course ${course.id} not available")
      val submissionsById = submissionsProvider.loadSubmissions(tasks, course.id)
      submissions.putAll(submissionsById)
      notifySubmissionsChanged()
      submissionsById.values.flatten()
    }
  }

  @Synchronized
  fun getCourseStateOnClose(): Map<Int, Submission> {
    val cachedStates = courseStateOnClose
    @Suppress("ReplaceIsEmptyWithIfEmpty")
    return if (cachedStates.isEmpty()) {
      val loadedStates = loadStateOnClose()
      courseStateOnClose = loadedStates
      loadedStates
    }
    else {
      cachedStates
    }
  }

  fun getSubmissions(task: Task): List<Submission>? {
    return submissions[task.id]
  }

  private fun loadStateOnClose(): Map<Int, Submission> {
    val course = course ?: error("Nullable Course")
    val submissionsProvider =
      SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: error("SubmissionProvider for course ${course.id} not available")
    return submissionsProvider.loadCourseStateOnClose(project, course)
  }

  fun getSubmissionWithSolutionText(task: Task, submissionId: Int): Submission? {
    val submission = getOrLoadSubmissions(task).find { it.id == submissionId }
                     ?: getCommunitySubmissionFromMemory(task.id, submissionId)
                     ?: return null

    if (submission is MarketplaceSubmission && submission.solutionFiles == null) {
      val course = course ?: return null
      val submissionsProvider = course.getSubmissionsProvider() ?: return null
      submissionsProvider.loadSolutionFiles(submission)
    }
    return submission
  }

  private fun getOrLoadSubmissions(task: Task): List<Submission> {
    val course = course ?: return emptyList()
    val submissionsProvider = course.getSubmissionsProvider() ?: return emptyList()
    val submissionsList = submissions[task.id]
    return if (submissionsList != null) {
      submissionsList
    }
    else {
      val loadedSubmissions = submissionsProvider.loadSubmissions(listOf(task), course.id)
      submissions.putAll(loadedSubmissions)
      notifySubmissionsChanged()
      return loadedSubmissions[task.id] ?: emptyList()
    }
  }

  fun addToSubmissions(taskId: Int, submission: Submission) {
    val submissionsList = submissions.getOrPut(taskId) { listOf(submission) }.toMutableList()
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      submissions[taskId] = submissionsList
      //potential race when loading submissions and checking task at one time
    }
    notifySubmissionsChanged()
  }

  private fun notifySubmissionsChanged() {
    project.messageBus.syncPublisher(TOPIC).submissionsChanged()
  }

  private fun notifySharedSolutionsUnchanged() {
    project.messageBus.syncPublisher(SHARED_SOLUTIONS_TOPIC).sharedSolutionsUnchanged()
  }

  fun containsCorrectSubmission(stepId: Int): Boolean {
    return getSubmissionsFromMemory(setOf(stepId)).any { it.status == CORRECT }
  }

  fun addToSubmissionsWithStatus(taskId: Int, checkStatus: CheckStatus, submission: Submission?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = checkStatus.rawStatus
    addToSubmissions(taskId, submission)
  }

  fun submissionsSupported(): Boolean {
    val course = course
    if (course == null) return false
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return false
    return submissionsProvider.areSubmissionsAvailable(course)
  }

  fun prepareSubmissionsContentWhenLoggedIn(loadSolutions: () -> Unit = {}) {
    val course = course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return
    val communitySubmissionsProvider = course.getCommunitySubmissionsProvider()

    CompletableFuture.runAsync({
      if (!isLoggedIn()) return@runAsync
      val taskToolWindowView = TaskToolWindowView.getInstance(project)
      val platformName = getPlatformName()

      taskToolWindowView.showLoadingSubmissionsPanel(platformName)
      taskToolWindowView.showLoadingCommunityPanel(platformName)
      loadSubmissionsContent(course, submissionsProvider, loadSolutions)
      communitySubmissionsProvider?.loadCommunityContent(course)

      notifySubmissionsChanged()
    }, ProcessIOExecutorService.INSTANCE)
  }

  fun removeCommunitySubmission(taskId: Int, submissionId: Int) {
    communitySubmissions[taskId]?.submissions?.removeIf { it.id == submissionId }
    notifySubmissionsChanged()
  }

  fun loadCommunitySubmissions(task: Task) {
    val course = course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return
    val communitySubmissionsProvider = course.getCommunitySubmissionsProvider() ?: return

    submissionsProvider.isLoggedInAsync().thenApply { isLoggedIn ->
      if (!isLoggedIn) return@thenApply

      val taskToolWindowView = TaskToolWindowView.getInstance(project)
      taskToolWindowView.showLoadingCommunityPanel(getPlatformName())
      val taskId = task.id
      val result = communitySubmissionsProvider.loadCommunitySubmissions(course, task)
      if (result == null) {
        communitySubmissions[taskId]?.hasMore = false
        return@thenApply taskToolWindowView.updateSubmissionsTab()
      }
      val (sharedSubmissions, hasMore) = result
      communitySubmissions[taskId] = TaskCommunitySubmissions(sharedSubmissions.toMutableList(), hasMore)
      notifySubmissionsChanged()
    }
  }

  fun loadMoreCommunitySubmissions(task: Task, latest: Int, oldest: Int) {
    val course = course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return
    val communitySubmissionsProvider = course.getCommunitySubmissionsProvider() ?: return

    submissionsProvider.isLoggedInAsync().thenApply { isLoggedIn ->
      if (!isLoggedIn) return@thenApply

      val taskToolWindowView = TaskToolWindowView.getInstance(project)
      taskToolWindowView.showLoadingCommunityPanel(getPlatformName())
      val taskId = task.id
      val result = communitySubmissionsProvider.loadMoreCommunitySubmissions(course, task, latest, oldest)
      if (result == null) {
        notifySharedSolutionsUnchanged()
        communitySubmissions[taskId]?.hasMore = false
        return@thenApply taskToolWindowView.updateSubmissionsTab()
      }
      communitySubmissions[taskId]?.let {
        it.submissions.addAll(result.submissions)
        it.hasMore = result.hasMore
      } ?: run {
        communitySubmissions[taskId] = result
      }
      notifySubmissionsChanged()
    }
  }

  private fun SubmissionsProvider.isLoggedInAsync(): CompletableFuture<Boolean> = CompletableFuture.supplyAsync({
    isLoggedIn()
  }, ProcessIOExecutorService.INSTANCE)

  private fun TaskToolWindowView.updateSubmissionsTab() {
    project.invokeLater {
      updateTab(TabType.SUBMISSIONS_TAB)
    }
  }

  fun deleteCourseSubmissionsLocally() {
    course?.allTasks?.forEach { submissions.remove(it.id) }
    notifySubmissionsChanged()
  }

  fun isLoggedIn(): Boolean = course?.getSubmissionsProvider()?.isLoggedIn() ?: false

  fun isSubmissionDownloadAllowed(): Boolean = UserAgreementSettings.getInstance().submissionsServiceAgreement

  fun isSolutionSharingAllowed(): Boolean = UserAgreementSettings.getInstance().submissionsServiceAgreement

  private fun getPlatformName(): String = course?.getSubmissionsProvider()?.getPlatformName() ?: error("Failed to get platform Name")

  fun doAuthorize() = course?.getSubmissionsProvider()?.doAuthorize(Runnable { prepareSubmissionsContentWhenLoggedIn() })

  private fun loadSubmissionsContent(course: Course, submissionsProvider: SubmissionsProvider, loadSolutions: () -> Unit) {
    submissions.putAll(submissionsProvider.loadAllSubmissions(course))
    loadSolutions()
  }

  private fun CommunitySubmissionsProvider.loadCommunityContent(course: Course) {
    val courseSharedSolutions = loadCommunitySubmissions(course)
    courseSharedSolutions.forEach { (taskId, sharedSolutions) ->
      communitySubmissions[taskId] = TaskCommunitySubmissions(sharedSolutions.toMutableList(), hasMore = true)
    }
  }

  private fun Course.getSubmissionsProvider(): SubmissionsProvider? {
    return SubmissionsProvider.getSubmissionsProviderForCourse(this)
  }

  fun hasMoreCommunitySubmissions(taskId: Int): Boolean {
    return communitySubmissions[taskId]?.hasMore ?: false
  }

  fun getLastSubmission(): Submission? {
    return submissions.values.flatten().sortedByDescending { it.time }.firstOrNull()
  }

  @TestOnly
  override fun cleanUpState() {
    submissions.clear()
  }

  @TestOnly
  fun addCommunitySolutions(taskId: Int, solutions: MutableList<Submission>) {
    communitySubmissions.putAll(mapOf(taskId to TaskCommunitySubmissions(solutions)))
  }

  companion object {

    @Topic.ProjectLevel
    val TOPIC: Topic<SubmissionsListener> = createTopic("Edu.submissions")

    @Topic.ProjectLevel
    val SHARED_SOLUTIONS_TOPIC: Topic<SharedSolutionsListener> = createTopic("Edu.sharedSolutions")

    fun getInstance(project: Project): SubmissionsManager {
      return project.service()
    }
  }
}

fun interface SubmissionsListener {
  fun submissionsChanged()
}

fun interface SharedSolutionsListener {
  fun sharedSolutionsUnchanged()
}