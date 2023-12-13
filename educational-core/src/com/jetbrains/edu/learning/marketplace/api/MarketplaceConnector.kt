package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.updateSettings.impl.PluginDownloader
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.DownloadUtil
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showErrorNotification
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showLogAction
import com.jetbrains.edu.coursecreator.CCNotificationUtils.showNotification
import com.jetbrains.edu.coursecreator.actions.marketplace.MarketplacePushCourse
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.ConnectorUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.marketplace.*
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showFailedToFindMarketplaceCourseOnRemoteNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showLoginNeededNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showMarketplaceAccountNotification
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils.showNoRightsToUpdateNotification
import com.jetbrains.edu.learning.marketplace.api.GraphqlQuery.LOADING_STEP
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle.message
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.CompletableFuture

abstract class MarketplaceConnector : MarketplaceAuthConnector(), CourseConnector {
  /**
   * It is recommended to use the static methods of the [com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount] class
   * instead of relying on the 'account' property. However, there are still certain things, such as the actual name of the JBA user,
   * that need to be retrieved from this property. Also, this property is used for retrieving Hub token.
   */
  override var account: MarketplaceAccount?
    get () = MarketplaceSettings.INSTANCE.getMarketplaceAccount()
    set(value) {
      MarketplaceSettings.INSTANCE.setAccount(value)
    }

  override val objectMapper: ObjectMapper by lazy {
    val objectMapper = ConnectorUtils.createRegisteredMapper(SimpleModule())
    objectMapper.addMixIn(EduCourse::class.java, MarketplaceEduCourseMixin::class.java)
    objectMapper
  }

  override val platformName: String = MARKETPLACE

  protected abstract val repositoryUrl: String

  private fun getRepositoryEndpoints(hubToken: String? = null): MarketplaceRepositoryEndpoints {
    return getEndpoints(baseUrl = repositoryUrl, accessToken = hubToken)
  }

  // Get requests:
  override fun getCurrentUserInfo(): JBAccountUserInfo? {
    return account?.userInfo
  }

  fun searchCourses(): List<EduCourse> {
    val allCourses = mutableListOf<EduCourse>()
    allCourses.addAll(searchCourses(false))
    if (isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_PRIVATE_COURSES)) {
      allCourses.addAll(searchCourses(true))
    }
    return allCourses
  }

  private fun searchCourses(searchPrivate: Boolean): List<EduCourse> {
    var offset = 0
    val courses = mutableListOf<EduCourse>()

    do {
      val coursesInfoList = getCoursesInfoList(offset, searchPrivate) ?: return courses
      val loadedCourses = updateFormatVersions(coursesInfoList)
      if (loadedCourses.isNullOrEmpty()) return courses
      courses.addAll(loadedCourses)
      offset += LOADING_STEP
    }
    while (courses.size < coursesInfoList.total)

    return courses
  }

  private fun updateFormatVersions(coursesInfoList: CoursesInfoList): List<EduCourse>? {
    val courses = coursesInfoList.courses.toMutableList()
    val updates = getUpdateInfoList(courses.map { it.id }) ?: return null
    val courseIdJsonVersionMap: Map<Int, Int> = updates.associateBy(UpdateInfo::pluginId) { it.compatibility.gte }
    for (course in courses) {
      val courseFormatVersion = courseIdJsonVersionMap[course.id]
      if (courseFormatVersion == null) {
        courses.remove(course)
        LOG.error("No UpdateInfo found for course ${course.name}")
        continue
      }
      course.formatVersion = courseFormatVersion
    }
    return courses
  }

  private fun getCoursesInfoList(offset: Int, searchPrivate: Boolean): CoursesInfoList? {
    val query = QueryData(GraphqlQuery.search(offset, searchPrivate))
    val response = getRepositoryEndpoints().search(query).executeHandlingExceptions()
    return response?.body()?.data?.myCoursesInfoList
  }

  fun searchCourse(courseId: Int, searchPrivate: Boolean = false): EduCourse? {
    val query = QueryData(GraphqlQuery.searchById(courseId, searchPrivate))
    val response = getRepositoryEndpoints().search(query).executeHandlingExceptions()
    val coursesInfoList = response?.body()?.data?.myCoursesInfoList ?: return null
    val course = updateFormatVersions(coursesInfoList)?.firstOrNull()
    course?.id = courseId
    return course
  }

  fun getLatestCourseUpdateInfo(courseId: Int): UpdateInfo? {
    val updateInfoList = getUpdateInfoList(listOf(courseId))
    if (updateInfoList == null) {
      error("Update info list for course $courseId is null")
    }
    else {
      return updateInfoList.firstOrNull()
    }
  }

  private fun getUpdateInfoList(courseIds: List<Int>): List<UpdateInfo>? {
    val response = getRepositoryEndpoints().getUpdateId(QueryData(GraphqlQuery.lastUpdatesList(courseIds))).executeHandlingExceptions()
    return response?.body()?.data?.updates?.updateInfoList
  }

  fun loadCourseStructure(course: EduCourse) {
    val jsonTextConverter = ToMemoryTextConverter //TODO should be read to disk with JsonTextToSQLiteConverter
    val unpackedCourse = loadCourse(course.id, jsonTextConverter)
    course.items = unpackedCourse.items
    course.additionalFiles = unpackedCourse.additionalFiles
    course.marketplaceCourseVersion = unpackedCourse.marketplaceCourseVersion
  }

  fun loadCourse(courseId: Int, jsonTextConverter: JsonTextConverter): EduCourse {
    val buildNumber = getBuildNumber()
    val uuid = PluginDownloader.getMarketplaceDownloadsUUID()
    val updateInfo = getLatestCourseUpdateInfo(courseId) ?: error("Update info for course $courseId is null")

    val link = "$repositoryUrl/plugin/download?updateId=${updateInfo.updateId}&uuid=$uuid&build=$buildNumber"
    val filePrefix = FileUtil.sanitizeFileName("marketplace-${courseId}")
    val tempFile = FileUtil.createTempFile(filePrefix, ".zip", true)
    DownloadUtil.downloadAtomically(null, link, tempFile)

    return EduUtilsKt.getLocalCourse(tempFile.path, jsonTextConverter) as? EduCourse
                         ?: error(message("dialog.title.failed.to.unpack.course"))
  }

  private fun uploadUnderProgress(message: String, uploadAction: () -> Unit) =
    ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtilsKt.execCancelable {
          uploadAction()
        }
      }, message, true, null)

  fun uploadNewCourseUnderProgress(project: Project, course: EduCourse, file: File, hubToken: String) {
    uploadUnderProgress(message("action.push.course")) {
      uploadNewCourse(project, course, file, hubToken)
    }
  }

  private fun uploadNewCourse(project: Project, course: EduCourse, file: File, hubToken: String) {
    LOG.info("Uploading new course from ${file.absolutePath}")

    val response = getRepositoryEndpoints(hubToken).uploadNewCourse(file.toMultipartBody(), LICENSE_URL.toPlainTextRequestBody())
      .executeUploadParsingErrors(project,
                                  message("notification.course.creator.failed.to.upload.course.title"),
                                  showLogAction,
                                  getOrganizationVendorPath(course.vendor?.name),
                                  {},
                                  {},
                                  {
                                    onAuthFailedActions(project, message("notification.course.creator.failed.to.upload.course.oauth.reason.title"))
                                  })
      .onError {
        LOG.error("Failed to upload course ${course.name}: $it")
        return
      }

    val courseBean = response.body()
    if (courseBean == null) {
      showErrorNotification(project, message("notification.course.creator.failed.to.upload.course.title"), action = showLogAction)
      return
    }
    course.id = courseBean.id
    YamlFormatSynchronizer.saveRemoteInfo(course)
    YamlFormatSynchronizer.saveItem(course)

    val message = message("marketplace.push.course.successfully.uploaded", courseBean.name)
    showNotification(project,
                     openOnMarketplaceAction(course.getMarketplaceUrl()),
                     message,
                     message("marketplace.push.course.successfully.uploaded.message"))
    LOG.info("$message with id ${courseBean.id}")
  }

  private fun onAuthFailedActions(project: Project, failedActionTitle: String) {
    account = null
    showLoginNeededNotification(project, message("item.upload.to.0.course.title", MARKETPLACE), failedActionTitle) { doAuthorize() }
  }

  private fun <T> Call<T>.executeUploadParsingErrors(project: Project,
                                                     failedActionMessage: String,
                                                     onErrorAction: AnAction,
                                                     vendorPath: String,
                                                     showPermissionDeniedNotification: () -> Unit,
                                                     showOnNotFoundCodeNotification: () -> Unit,
                                                     onAuthFailedActions: () -> Unit): Result<Response<T>, String> {
    val response = executeCall().onError { return Err(it) }
    val responseCode = response.code()
    val errorBody = response.errorBody()?.string() ?: return Ok(response)
    val errorMessage = "$errorBody Code $responseCode"
    return when (responseCode) {
      HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED -> Ok(response)
      HttpURLConnection.HTTP_BAD_REQUEST -> {
        Err(errorMessage) // 400
      }
      HttpURLConnection.HTTP_FORBIDDEN -> {
        when {
          errorBody.contains(ERROR_MARKETPLACE_AGREEMENT_NOT_ACCEPTED) ||
          errorBody.contains(ERROR_PLUGIN_DEV_AGREEMENT_NOT_ACCEPTED) -> showMarketplaceAccountNotification(
            project,
            message("marketplace.plugin.development.agreement.not.accepted")
          ) { openOnMarketplaceAction(MARKETPLACE_CREATE_VENDOR_PATH) }

          errorBody.contains(ERROR_TRADER_STATUS_NOT_SPECIFIED) -> showMarketplaceAccountNotification(
            project,
            message("marketplace.trader.status.not.specified")
          ) { openOnMarketplaceAction(MARKETPLACE_PROFILE_PATH) }

          errorBody.contains(ERROR_CREATE_VENDOR_ACCOUNT) -> showMarketplaceAccountNotification(
            project,
            message("marketplace.create.vendor.account")
          ) { openOnMarketplaceAction(MARKETPLACE_CREATE_VENDOR_PATH) }

          errorBody.contains(ERROR_ORGANIZATION_TRADER_STATUS_NOT_SPECIFIED) -> {
              showMarketplaceAccountNotification(
              project,
              message("marketplace.organization.trader.status.not.specified")
            ) { openOnMarketplaceAction(vendorPath) }
          }

          errorBody.contains(ERROR_PERMISSION_DENIED) -> showPermissionDeniedNotification()
          errorBody.contains(ERROR_AUTH_FAILED) -> onAuthFailedActions()
        }

        Err(errorMessage) // 403
      }
      HttpURLConnection.HTTP_NOT_FOUND -> {
          showOnNotFoundCodeNotification()
          Err(errorMessage) //404
        }
      HttpURLConnection.HTTP_UNAVAILABLE, HttpURLConnection.HTTP_BAD_GATEWAY -> {
        showErrorNotification(project, failedActionMessage, action = onErrorAction)
        Err("${EduFormatBundle.message("error.service.maintenance")}\n\n$errorMessage") // 502, 503
      }
      in HttpURLConnection.HTTP_INTERNAL_ERROR..HttpURLConnection.HTTP_VERSION -> {
        showErrorNotification(project, failedActionMessage, action = onErrorAction)
        Err("${EduFormatBundle.message("error.service.down")}\n\n$errorMessage") // 500x
      }
      else -> {
        LOG.warn("Code $responseCode is not handled")
        showErrorNotification(project, failedActionMessage, action = onErrorAction)
        Err(EduFormatBundle.message("error.unexpected.error", errorMessage))
      }
    }
  }

  private fun EduCourse.getMarketplaceUrl() = "$MARKETPLACE_PLUGIN_URL/${this.id}"

  private fun openOnMarketplaceAction(link: String): AnAction {
    return object : AnAction(message("action.open.on.text", MARKETPLACE)) {
      override fun actionPerformed(e: AnActionEvent) {
        EduBrowser.getInstance().browse(link)
      }
    }
  }

  fun uploadCourseUpdateUnderProgress(project: Project, course: EduCourse, file: File, hubToken: String) {
    var courseVersionMismatch = false
    invokeAndWaitIfNeeded {
      uploadUnderProgress(message("push.course.updating.progress.title")) {
        courseVersionMismatch = uploadCourseUpdate(project, course, file, hubToken)
      }
    }
    if (courseVersionMismatch) {
      val updateActionTitle = message("item.update.on.0.course.title", MARKETPLACE)
      val insertedCourseVersion = createAndShowCourseVersionDialog(project, course, updateActionTitle)
                                  ?: return
      course.marketplaceCourseVersion = insertedCourseVersion
      YamlFormatSynchronizer.saveRemoteInfo(course)
      val pushAction = ActionManager.getInstance().getAction(MarketplacePushCourse.ACTION_ID)
      pushAction.templatePresentation.text = updateActionTitle
      showNotification(project, message("marketplace.inserted.course.version.notification", insertedCourseVersion), pushAction)
    }
  }

  private fun uploadCourseUpdate(project: Project, course: EduCourse, file: File, hubToken: String): Boolean {
    LOG.info("Uploading course update from ${file.absolutePath}")
    val uploadAsNewCourseAction: AnAction = NotificationAction.createSimpleExpiring(
      message("notification.course.creator.access.denied.action")) {
      course.convertToLocal()
      uploadNewCourseUnderProgress(project, course, file, hubToken)
    }

    getRepositoryEndpoints(hubToken).uploadCourseUpdate(file.toMultipartBody(), course.id)
      .executeUploadParsingErrors(project,
                                  message("notification.course.creator.failed.to.update.course.title"),
                                  uploadAsNewCourseAction,
                                  getOrganizationVendorPath(course.vendor?.name),
                                  {
                                    showNoRightsToUpdateNotification(project, course) {
                                      uploadNewCourseUnderProgress(project, course, file, hubToken)
                                    }
                                  },
                                  {
                                    showFailedToFindMarketplaceCourseOnRemoteNotification(project, uploadAsNewCourseAction)
                                  },
                                  {
                                    onAuthFailedActions(project, message("notification.course.creator.failed.to.update.course.oauth.reason.title"))
                                  })
      .onError {
        val message = "Failed to upload course update for course ${course.id}: ${it}"
        if (it.contains(PLUGIN_CONTAINS_VERSION_ERROR_TEXT)) {
          LOG.info(message)
          return true
        }
        LOG.error(message)
        return false
      }

    val message = message("marketplace.push.course.successfully.updated", course.name, course.marketplaceCourseVersion)
    showNotification(project, message, openOnMarketplaceAction(course.getMarketplaceUrl()))
    LOG.info(message)
    YamlFormatSynchronizer.saveItem(course)
    return false
  }

  /**
   * the following link formats are supported:
   * https://plugins.jetbrains.com/plugin/12345 link with numeric id
   * https://plugins.jetbrains.com/plugin/12345-plugin-name link with xmlId
   * 12345-plugin-name just a plugin xmlId
   */
  override fun getCourseIdFromLink(link: String): Int {
    if (link.matches(XML_ID)) {
      return parseXmlId(link)
    }
    try {
      val url = URL(link)
      val pathParts = url.path.split("/").dropLastWhile { it.isEmpty() }
      for (i in pathParts.indices) {
        val part = pathParts[i]
        if (part == "plugin" && i + 1 < pathParts.size) {
          val xmlId = pathParts[i + 1]
          return parseXmlId(xmlId)
        }
      }
    }
    catch (e: MalformedURLException) {
      LOG.warn(e.message)
    }

    return -1
  }

  override fun getCourseInfoByLink(link: String): EduCourse? {
    return getCourseInfoByLink(link, isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_PRIVATE_COURSES))
  }

  fun getCourseInfoByLink(link: String, searchPrivate: Boolean): EduCourse? {
    val courseId = link.toIntOrNull() ?: getCourseIdFromLink(link)
    if (courseId != -1) {
      // we don't know beforehand if the course to be searched for is private or public, while private and public courses
      // need different templates to be found via graphql
      return searchCourse(courseId, false) ?: if (searchPrivate) searchCourse(courseId, true) else null
    }
    return null
  }

  private fun parseXmlId(xmlId: String): Int {
    return try {
      Integer.parseInt(xmlId.split("-")[0])
    }
    catch (e: NumberFormatException) {
      -1
    }
  }


  @Suppress("DialogTitleCapitalization")
  private fun createAndShowCourseVersionDialog(project: Project, course: EduCourse, failedActionTitle: String): Int? {
    val currentCourseVersion = course.marketplaceCourseVersion
    val suggestedCourseVersion = currentCourseVersion + 1

    return invokeAndWaitIfNeeded {
      Messages.showInputDialog(project,
                               message("marketplace.insert.course.version.dialog", currentCourseVersion, course.name, failedActionTitle),
                               message("marketplace.insert.course.version.dialog.title"),
                               null,
                               suggestedCourseVersion.toString(),
                               NumericInputValidator(message("marketplace.insert.course.version.validation.empty"),
                                                     message("marketplace.insert.course.version.validation.not.numeric")))?.toIntOrNull()
    }
  }

  fun isLoggedInAsync(): CompletableFuture<Boolean> {
    return CompletableFuture.supplyAsync({ isLoggedIn() }, ProcessIOExecutorService.INSTANCE)
  }

  companion object {
    private val LOG = logger<MarketplaceConnector>()

    private val XML_ID = "\\d{5,}-.*".toRegex()
    private const val PLUGIN_CONTAINS_VERSION_ERROR_TEXT = "plugin already contains version"

    private const val ERROR_MARKETPLACE_AGREEMENT_NOT_ACCEPTED = "You have not accepted the JetBrains Plugin Marketplace agreement"
    private const val ERROR_PLUGIN_DEV_AGREEMENT_NOT_ACCEPTED = "You have not accepted the Plugin Developer agreement"
    private const val ERROR_TRADER_STATUS_NOT_SPECIFIED = "Please specify the trader status for your vendor"
    private const val ERROR_CREATE_VENDOR_ACCOUNT = "Please create a vendor account before uploading a new plugin"
    private const val ERROR_ORGANIZATION_TRADER_STATUS_NOT_SPECIFIED = "Please create a vendor account before uploading a new plugin"
    private const val ERROR_PERMISSION_DENIED = "Unfortunately, you don't have sufficient permissions"
    private const val ERROR_AUTH_FAILED = "Authentication Failed"
    fun getInstance(): MarketplaceConnector = service()
  }
}