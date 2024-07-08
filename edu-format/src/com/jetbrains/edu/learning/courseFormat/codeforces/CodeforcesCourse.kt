package com.jetbrains.edu.learning.courseFormat.codeforces

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODEFORCES
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODEFORCES_URL
import java.time.Duration
import java.time.ZonedDateTime
import java.util.*

open class CodeforcesCourse : Course {
  var endDateTime: ZonedDateTime? = null
  var startDate: ZonedDateTime? = null
  var length: Duration = Duration.ZERO
  var registrationLink: String? = null
  var availableLanguages: List<String> = emptyList()
  var programTypeId: String? = null
    get() = if (field != null) field else CodeforcesTask.codeforcesDefaultProgramTypeId(this).toString()

  var participantsNumber: Int = 0
  var registrationCountdown: Duration? = null
  var standingsLink: String? = null
  var remainingTime: Duration? = Duration.ZERO

  val isUpcomingContest: Boolean get() = registrationCountdown != null || isOngoing
  val isRegistrationOpen: Boolean get() = registrationLink != null
  val isPastContest: Boolean get() = !isUpcomingContest

  val isOngoing: Boolean
    get() = (startDate != null && ZonedDateTime.now().isAfter(startDate)) && (endDateTime?.isAfter(ZonedDateTime.now()) == true)

  //used for deserialization
  constructor()

  constructor(contestParameters: ContestParameters) {
    setContestParameters(contestParameters)
  }

  private fun setContestParameters(contestParameters: ContestParameters) {
    id = "" + contestParameters.id
    languageId = contestParameters.languageId
    languageVersion = contestParameters.languageVersion
    languageCode = contestParameters.locale
    programTypeId = contestParameters.programTypeId
    endDateTime = contestParameters.endDateTime
    updateDate = Date()
    startDate = contestParameters.startDate
    length = contestParameters.length
    registrationLink = contestParameters.registrationLink
    registrationCountdown = contestParameters.registrationCountdown
    availableLanguages = contestParameters.availableLanguages
    name = contestParameters.name
    participantsNumber = contestParameters.participantsNumber
    standingsLink = contestParameters.standingsLink
    remainingTime = contestParameters.remainingTime
    authors = contestParameters.authors
  }

  override val itemType: String = CODEFORCES

  fun getContestUrl(): String = "$CODEFORCES_URL/contest/$id"
}