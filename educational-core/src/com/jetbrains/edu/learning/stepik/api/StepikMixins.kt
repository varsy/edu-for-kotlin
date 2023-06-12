@file:Suppress("unused", "PropertyName") // used for json serialization

package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NAME
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.submissions.IS_VISIBLE
import com.jetbrains.edu.learning.submissions.PLACEHOLDERS
import com.jetbrains.edu.learning.submissions.STATUS
import com.jetbrains.edu.learning.submissions.TEXT
import java.util.*

const val IS_IDEA_COMPATIBLE = "is_idea_compatible"
const val IS_ADAPTIVE = "is_adaptive"
const val COURSE_FORMAT = "course_format"
const val SECTIONS = "sections"
const val INSTRUCTORS = "instructors"
const val UPDATE_DATE = "update_date"
const val IS_PUBLIC = "is_public"
const val SUMMARY = "summary"
const val TITLE = "title"
const val PROGRAMMING_LANGUAGE = "programming_language"
const val LANGUAGE = "language"
const val ADMINS_GROUP = "admins_group"
const val LEARNERS_COUNT = "learners_count"
const val REVIEW_SUMMARY = "review_summary"
const val UNITS = "units"
const val COURSE = "course"
const val POSITION = "position"
const val STEPS = "steps"
const val NUMBER = "number"
const val OFFSET = "offset"
const val LENGTH = "length"
const val DEPENDENCY = "dependency"
const val POSSIBLE_ANSWER = "possible_answer"
const val PLACEHOLDER_TEXT = "placeholder_text"
const val SELECTED = "selected"
const val SECTION = "section"
const val LESSON = "lesson"
const val TASK = "task"
private const val FILE = "file"
const val PLACEHOLDER = "placeholder"
const val STEPIK_ID = "stepic_id"
const val FILES = "files"
const val CHOICE_VARIANTS = "choice_variants"
const val IS_MULTICHOICE = "is_multichoice"
const val SELECTED_VARIANTS = "selected_variants"
const val CUSTOM_NAME = "custom_name"
const val SOLUTION_HIDDEN = "solution_hidden"
private const val TASK_TYPE = "task_type"
private const val ENVIRONMENT_SEPARATOR = "#"

open class StepikItemMixin {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  lateinit var updateDate: Date
}

@JsonDeserialize(builder = StepikCourseBuilder::class)
abstract class StepikEduCourseMixin

@JsonPOJOBuilder(withPrefix = "")
private class StepikCourseBuilder(
  @JsonProperty(ID) val id: Int,
  @JsonProperty(UPDATE_DATE) val updateDate: Date,
  @JsonProperty(IS_IDEA_COMPATIBLE) val ideaCompatible: Boolean,
  @JsonProperty(IS_ADAPTIVE) val isAdaptive: Boolean,
  @JsonProperty(COURSE_FORMAT) val courseFormat: String,
  @JsonProperty(SECTIONS) val sectionIds: List<Int>?,
  @JsonProperty(INSTRUCTORS) val instructors: List<Int>?,
  @JsonProperty(IS_PUBLIC) val isPublic: Boolean,
  @JsonProperty(SUMMARY) val summary: String,
  @JsonProperty(TITLE) val title: String,
  @JsonProperty(LANGUAGE) val languageCode: String,
  @JsonProperty(LEARNERS_COUNT) val learnersCount: Int = 0,
  @JsonProperty(REVIEW_SUMMARY) val reviewSummary: Int = 0
) {
  private fun build(): Course {
    val course = if (ideaCompatible) {
      EduCourse()
    }
    else {
      StepikCourse().also {
        it.isAdaptive = isAdaptive
      }
    }

    course.also {
      it.id = id
      it.updateDate = updateDate
      it.sectionIds = sectionIds ?: emptyList()
      it.instructors = instructors ?: emptyList()
      it.isStepikPublic = isPublic
      it.description = summary
      it.name = title
      it.languageCode = languageCode
      it.learnersCount = learnersCount
      it.reviewSummary = reviewSummary
    }
    course.setCourseFormat(courseFormat)
    return course
  }
}

private fun EduCourse.setCourseFormat(courseFormat: String) {
  // courseFormat format: "pycharm<version> <language>$ENVIRONMENT_SEPARATOR<environment>"
  val languageIndex = courseFormat.indexOf(" ")
  if (languageIndex == -1) {
    LOG.info("Language for course `$name` with `$courseFormat` type can't be set because it isn't `pycharm` course")
    return
  }
  val environmentIndex = courseFormat.indexOf(ENVIRONMENT_SEPARATOR, languageIndex + 1)
  if (environmentIndex != -1) {
    languageId = courseFormat.substring(languageIndex + 1, environmentIndex)
    environment = courseFormat.substring(environmentIndex + 1)
  }
  else {
    languageId = courseFormat.substring(languageIndex + 1)
  }

  if (courseFormat.contains(StepikNames.PYCHARM_PREFIX)) {
    val formatVersionString = courseFormat.substring(StepikNames.PYCHARM_PREFIX.length, languageIndex)
    formatVersion = try {
      formatVersionString.toInt()
    }
    catch (e: NumberFormatException) {
      JSON_FORMAT_VERSION
    }
  }
}

class StepikLessonMixin : StepikItemMixin() {
  @JsonProperty(STEPS)
  lateinit var stepIds: MutableList<Int>

  @JsonProperty(TITLE)
  lateinit var name: String

}

@JsonPropertyOrder(NAME, IS_VISIBLE, TEXT)
open class StepikEduFileMixin {
  @JsonProperty(NAME)
  lateinit var name: String

  @JsonProperty(IS_VISIBLE)
  var isVisible = true

  lateinit var text: String
    @JsonProperty(TEXT)
    @JsonDeserialize(using = TaskFileTextDeserializer::class)
    get
    @JsonProperty(TEXT)
    set
}

@JsonPropertyOrder(NAME, PLACEHOLDERS, IS_VISIBLE, TEXT)
class StepikTaskFileMixin : StepikEduFileMixin() {
  @JsonProperty(PLACEHOLDERS)
  lateinit var _answerPlaceholders: MutableList<AnswerPlaceholder>
}

class StepikAnswerPlaceholderMixin {
  @JsonProperty(OFFSET)
  var offset = -1

  @JsonProperty(LENGTH)
  private var length = -1

  @JsonProperty(DEPENDENCY)
  lateinit var placeholderDependency: AnswerPlaceholderDependency

  @JsonProperty(POSSIBLE_ANSWER)
  lateinit var possibleAnswer : String

  @JsonProperty(PLACEHOLDER_TEXT)
  lateinit var placeholderText: String

  @JsonProperty(SELECTED)
  private var selected = false
}

@JsonPropertyOrder(NAME, STEPIK_ID, STATUS, FILES, TASK_TYPE)
open class StepikTaskMixin {
  @JsonProperty(NAME)
  var name: String? = null

  @JsonProperty(STATUS)
  var checkStatus = CheckStatus.Unchecked

  @JsonProperty(STEPIK_ID)
  private var id: Int = 0

  @JsonProperty(FILES)
  private var _taskFiles: MutableMap<String, TaskFile> = LinkedHashMap()

  val itemType: String
    @JsonProperty(TASK_TYPE)
    get() = throw NotImplementedInMixin()
}

class TaskFileTextDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<String>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): String {
    return StringUtil.convertLineSeparators(jp.valueAsString)
  }
}

private val LOG: Logger = logger<StepikConnector>()