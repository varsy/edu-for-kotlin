package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.MARKETPLACE
import java.util.*

/**
 * Used in the following Mixins:
 * - [com.jetbrains.edu.coursecreator.actions.mixins.RemoteEduCourseMixin]
 * - [com.jetbrains.edu.learning.marketplace.api.MarketplaceEduCourseMixin]
 * - [com.jetbrains.edu.learning.stepik.api.StepikEduCourseMixin]
 * - [com.jetbrains.edu.learning.yaml.format.EduCourseRemoteInfoYamlMixin]
 * - [com.jetbrains.edu.learning.yaml.format.CourseYamlMixin]
 */
open class EduCourse : Course() {
  @Transient
  var isUpToDate: Boolean = true
  var learnersCount: Int = 0
  var reviewScore: Double = 0.0

  var generatedEduId: String? = null

  // in CC mode is used to store top-level lessons' section id
  var sectionIds: List<Int> = emptyList()
  var instructors: List<Int> = emptyList()
  var isStepikPublic: Boolean = false
  var reviewSummary: Int = 0

  override val itemType: String
    get() = if (isMarketplace) MARKETPLACE else super.itemType

  var formatVersion: Int = JSON_FORMAT_VERSION

  override val isStepikRemote: Boolean
    get() = id != "0" && !isMarketplace

  val isMarketplaceRemote: Boolean
    get() = id != "0" && isMarketplace

  fun convertToLocal() {
    if (isMarketplace) {
      marketplaceCourseVersion = 1
    }
    else {
      isStepikPublic = false
      sectionIds = emptyList()
      instructors = emptyList()
    }
    id = "0"
    updateDate = Date(0)
  }

  var isPreview: Boolean = false

}
