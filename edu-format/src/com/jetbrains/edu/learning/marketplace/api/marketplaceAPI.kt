package com.jetbrains.edu.learning.marketplace.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.MARKETPLACE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SOLUTION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.STATES_ON_CLOSE
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SUBMISSIONS
import com.jetbrains.edu.learning.submissions.SolutionFile
import com.jetbrains.edu.learning.submissions.Submission
import org.jetbrains.annotations.TestOnly

const val ID = "id"
const val NAME = "name"
private const val COMPATIBILITY = "compatibility"
private const val FORMAT_VERSION = "format_version"
private const val UPDATE_VERSION = "update_version"
private const val DATA = "data"
private const val ENVIRONMENT = "environment"
private const val GTE = "gte"
private const val IS_PRIVATE = "isPrivate"
private const val LANGUAGE = "language"
private const val PLUGIN_ID = "pluginId"
private const val PLUGINS = "plugins"
private const val PROGRAMMING_LANGUAGE = "programmingLanguage"
private const val PROGRAMMING_LANGUAGE_ID = "programmingLanguageId"
private const val PROGRAMMING_LANGUAGE_VERSION = "programmingLanguageVersion"
private const val QUERY = "query"
private const val SOLUTION_AWS_KEY = "solution_aws_key"
private const val TASK_ID = "task_id"
private const val TEST_RESULTS = "test_results"
private const val TOTAL = "total"
private const val UPDATES = "updates"
private const val VERSION = "version"
private const val HAS_NEXT = "has_next"

class MarketplaceAccount : OAuthAccount<JBAccountUserInfo> {
  @TestOnly
  constructor() : super()

  constructor(jbAccountUserInfo: JBAccountUserInfo) : super(jbAccountUserInfo)

  override val servicePrefix: String = MARKETPLACE

  fun checkTheSameUserAndUpdate(currentJbaUser: JBAccountUserInfo): Boolean {
    return if (userInfo.jbaLogin == currentJbaUser.jbaLogin) {
      //update username and email in case changed on remote
      userInfo.email = currentJbaUser.email
      userInfo.name = currentJbaUser.name
      true
    }
    else {
      false
    }
  }
}

class QueryData(graphqlQuery: String) {
  @JsonProperty(QUERY)
  var query: String = graphqlQuery
}

// downloaded CoursesInfos are not full-fledged EduCourses, they miss information, specific for the update,
// contained in the UpdateInfo (e.g. Course.formatVersion, stored in Compatibility.gte)
@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesInfoList {
  @JsonProperty(TOTAL)
  var total: Int = -1

  @JsonProperty(PLUGINS)
  var courses: List<EduCourse> = emptyList()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesData {
  @JsonProperty(DATA)
  lateinit var data: CoursesInfos
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CoursesInfos {
  @JsonProperty(PLUGINS)
  lateinit var myCoursesInfoList: CoursesInfoList
}

@Suppress("unused")
class Author {
  @JsonProperty(NAME)
  var name: String = ""

  constructor()
  constructor(name: String) {
    this.name = name
  }
}

class Fields {
  @Suppress("unused")
  @Deprecated("Use languageId and languageVersion instead")
  @JsonProperty(PROGRAMMING_LANGUAGE)
  var programmingLanguage: String = ""

  @JsonProperty(PROGRAMMING_LANGUAGE_ID)
  var languageId: String? = null

  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION)
  var languageVersion: String? = null

  @JsonProperty(LANGUAGE)
  var language: String = ""

  @JsonProperty(ENVIRONMENT)
  var environment: String? = DEFAULT_ENVIRONMENT

  @JsonProperty(IS_PRIVATE)
  var isPrivate: Boolean = false
}

class Organization {
  @JsonProperty(NAME)
  var name: String? = ""
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateData {
  @JsonProperty(DATA)
  lateinit var data: Updates
}

@Suppress("unused")
@JsonIgnoreProperties(ignoreUnknown = true)
class Updates {
  @JsonProperty(TOTAL)
  var total: Int = 0

  @JsonProperty(UPDATES)
  lateinit var updates: UpdatesList
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdatesList {
  @JsonProperty(UPDATES)
  lateinit var updateInfoList: List<UpdateInfo>
}

@JsonIgnoreProperties(ignoreUnknown = true)
class UpdateInfo {
  @JsonProperty(ID)
  var updateId: Int = -1

  @JsonProperty(PLUGIN_ID)
  var pluginId: Int = -1

  @JsonProperty(VERSION)
  var version: Int = -1

  @JsonProperty(COMPATIBILITY)
  var compatibility: Compatibility = Compatibility()
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Compatibility {
  @JsonProperty(GTE)
  var gte: Int = -1
}

@JsonIgnoreProperties(ignoreUnknown = true)
class CourseBean {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(NAME)
  var name: String = ""
}

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
sealed class UploadResponse

@JsonIgnoreProperties(ignoreUnknown = true)
data class SuccessCourseUploadResponse(
  val warnings: List<String>,
  val plugin: CourseBean
): UploadResponse()

@JsonIgnoreProperties(ignoreUnknown = true)
data class SuccessCourseUpdateUploadResponse(
  val warnings: List<String>,
  val update: UpdateInfo
): UploadResponse()

@JsonIgnoreProperties(ignoreUnknown = true)
data class FailedCourseUploadResponse(
  val warnings: List<String>,
  val errors: List<String>
): UploadResponse() {
  companion object {
    @JvmStatic
    fun parse(objectMapper: ObjectMapper, content: String): FailedCourseUploadResponse {
      return try {
        objectMapper.readValue(content, FailedCourseUploadResponse::class.java)
      }
      catch (_: Exception) {
        FailedCourseUploadResponse(emptyList(), listOf("Unknown error occurred: $content"))
      }
    }
  }
}

abstract class MarketplaceSubmissionBase : Submission() {
  @JsonProperty(UPDATE_VERSION)
  var courseVersion: Int = 0

  @JsonProperty(TASK_ID)
  override var taskId: Int = -1

  // used in GET requests: solution files are being loaded from s3 on demand
  @JsonIgnore
  override var solutionFiles: List<SolutionFile>? = null

  @JsonProperty(FORMAT_VERSION)
  override var formatVersion: Int = JSON_FORMAT_VERSION

  @JsonProperty(SOLUTION_AWS_KEY)
  var solutionKey: String = ""
}

class MarketplaceSubmission() : MarketplaceSubmissionBase() {
  // used in POST requests: we send solution files written as a string to submissions service and never download back
  @JsonProperty(SOLUTION)
  var solution: String = ""

  @JsonProperty(TEST_RESULTS)
  var testsInfo: List<EduTestInfo> = emptyList()

  constructor(
    taskId: Int,
    checkStatus: CheckStatus,
    solutionText: String,
    solutionFiles: List<SolutionFile>?,
    courseVersion: Int,
    testsInfo: List<EduTestInfo> = emptyList()
  ) : this() {
    this.taskId = taskId
    this.status = checkStatus.rawStatus
    this.solutionFiles = solutionFiles
    this.courseVersion = courseVersion
    solution = solutionText
    this.testsInfo = testsInfo
  }
}

class MarketplaceStateOnClose : MarketplaceSubmissionBase()

class MarketplaceSubmissionsList {
  @JsonProperty(HAS_NEXT)
  var hasNext: Boolean = false

  @JsonProperty(SUBMISSIONS)
  lateinit var submissions: List<MarketplaceSubmission>
}

data class MarketplaceStateOnCloseList(
  @JsonProperty(HAS_NEXT) val hasNext: Boolean = false,
  @JsonProperty(STATES_ON_CLOSE) val states: List<MarketplaceStateOnClose>
)

data class MarketplaceStateOnClosePost(
  @JsonProperty(TASK_ID) val taskId: Int,
  @JsonProperty(SOLUTION) val solution: String,
  @JsonProperty(FORMAT_VERSION) val formatVersion: Int = JSON_FORMAT_VERSION
)