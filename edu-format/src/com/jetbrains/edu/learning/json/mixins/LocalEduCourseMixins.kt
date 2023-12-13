@file:JvmName("LocalEduCourseMixins")
@file:Suppress("unused")

package com.jetbrains.edu.learning.json.mixins

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.MARKETPLACE
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.json.encrypt.Encrypt
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.AUTHORS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.CHOICE_OPTIONS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.CUSTOM_NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.DEPENDENCY
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.DESCRIPTION_FORMAT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.DESCRIPTION_TEXT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ENVIRONMENT_SETTINGS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FEEDBACK_LINK
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FILE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FILES
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.JSON_TEXT_CONVERTER
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.FRAMEWORK_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_BINARY
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.HIGHLIGHT_LEVEL
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_EDITABLE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_MULTIPLE_CHOICE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_VISIBLE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ITEMS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ITEM_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.LANGUAGE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.LENGTH
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.LESSON
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.MAX_VERSION
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.MESSAGE_CORRECT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.MESSAGE_INCORRECT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.MIN_VERSION
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.OFFSET
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLACEHOLDER
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLACEHOLDERS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLACEHOLDER_TEXT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLUGINS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLUGIN_ID
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PLUGIN_NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.POSSIBLE_ANSWER
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_ID
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_VERSION
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.QUIZ_HEADER
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.SECTION
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.SOLUTIONS_HIDDEN
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.SOLUTION_HIDDEN
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.SUMMARY
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TAGS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TASK
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TASK_LIST
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TASK_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TEXT
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TITLE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.VERSION

private val LOG = logger<LocalEduCourseMixin>()

@JsonPropertyOrder(
  ENVIRONMENT, SUMMARY, TITLE, PROGRAMMING_LANGUAGE_ID, PROGRAMMING_LANGUAGE_VERSION, LANGUAGE,
  COURSE_TYPE, SOLUTIONS_HIDDEN, PLUGINS, ITEMS, AUTHORS, TAGS, ADDITIONAL_FILES, VERSION
)
abstract class LocalEduCourseMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(AUTHORS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @JsonSerialize(contentConverter = UserInfoToString::class)
  @JsonDeserialize(contentConverter = MarketplaceUserInfoFromString::class)
  private var authors: MutableList<UserInfo> = ArrayList()

  @JsonProperty(SUMMARY)
  private lateinit var description: String

  @Suppress("SetterBackingFieldAssignment")
  private var programmingLanguage: String? = null
    @JsonProperty(PROGRAMMING_LANGUAGE)
    set(value) {
      if (formatVersion >= JSON_FORMAT_VERSION_WITH_NEW_LANGUAGE_VERSION || value.isNullOrEmpty()) return
      value.split(" ").apply {
        languageId = first()
        languageVersion = getOrNull(1)
      }
    }

  @JsonProperty(PROGRAMMING_LANGUAGE_ID)
  private lateinit var languageId: String

  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION)
  private var languageVersion: String? = null

  @JsonProperty(LANGUAGE)
  private lateinit var languageCode: String

  @JsonProperty(ENVIRONMENT)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var environment: String

  @JsonProperty(ENVIRONMENT_SETTINGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  lateinit var environmentSettings: Map<String, String>

  val itemType: String
    @JsonProperty(COURSE_TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(ITEMS)
  private lateinit var _items: List<StudyItem>

  @JsonProperty(ADDITIONAL_FILES)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var additionalFiles: List<EduFile>

  @JsonProperty(PLUGINS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var pluginDependencies: List<PluginInfo>

  @JsonProperty(SOLUTIONS_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var solutionsHidden: Boolean = false

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>

  @JsonProperty(VERSION)
  private var formatVersion = JSON_FORMAT_VERSION
}

private class UserInfoToString : StdConverter<UserInfo, String?>() {
  override fun convert(value: UserInfo?): String? = value?.getFullName()
}

private class MarketplaceUserInfoFromString : StdConverter<String?, JBAccountUserInfo?>() {
  override fun convert(value: String?): JBAccountUserInfo? {
    if (value == null) {
      return null
    }
    return JBAccountUserInfo(value)
  }
}

abstract class PluginInfoMixin : PluginInfo() {
  @JsonProperty(PLUGIN_ID)
  override var stringId: String = ""

  @JsonProperty(PLUGIN_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var displayName: String? = null

  @JsonProperty(MIN_VERSION)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var minVersion: String? = null

  @JsonProperty(MAX_VERSION)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var maxVersion: String? = null
}

@JsonPropertyOrder(TITLE, CUSTOM_NAME, TAGS, ITEMS, TYPE)
abstract class LocalSectionMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(ITEMS)
  private lateinit var _items: List<StudyItem>

  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(TITLE, CUSTOM_NAME, TAGS, TASK_LIST, TYPE)
abstract class LocalLessonMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(TASK_LIST)
  private lateinit var _items: List<StudyItem>

  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(
  NAME, CUSTOM_NAME, TAGS, FILES, DESCRIPTION_TEXT, DESCRIPTION_FORMAT, FEEDBACK_LINK, SOLUTION_HIDDEN,
  TASK_TYPE
)
abstract class LocalTaskMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(FILES)
  private lateinit var _taskFiles: MutableMap<String, TaskFile>

  @JsonProperty(DESCRIPTION_TEXT)
  private lateinit var descriptionText: String

  @JsonProperty(DESCRIPTION_FORMAT)
  private lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty(FEEDBACK_LINK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private lateinit var feedbackLink: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var solutionHidden: Boolean? = null

  val itemType: String
    @JsonProperty(TASK_TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(
  CHOICE_OPTIONS, IS_MULTIPLE_CHOICE, MESSAGE_CORRECT, MESSAGE_INCORRECT, QUIZ_HEADER,
  NAME, CUSTOM_NAME, TAGS, FILES, DESCRIPTION_TEXT, DESCRIPTION_FORMAT, FEEDBACK_LINK, SOLUTION_HIDDEN, TASK_TYPE
)
abstract class ChoiceTaskLocalMixin : LocalTaskMixin() {

  @JsonProperty
  private var isMultipleChoice: Boolean = false

  @JsonProperty
  private lateinit var choiceOptions: List<ChoiceOption>

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FeedbackCorrectFilter::class)
  @JsonProperty
  private lateinit var messageCorrect: String

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FeedbackIncorrectFilter::class)
  @JsonProperty
  private lateinit var messageIncorrect: String

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = QuizHeaderFilter::class)
  @JsonProperty
  private lateinit var quizHeader: String
}

abstract class ChoiceOptionLocalMixin {
  @JsonProperty
  private var text: String = ""

  @JsonProperty
  private var status: ChoiceOptionStatus = ChoiceOptionStatus.UNKNOWN
}

@JsonPropertyOrder(NAME, IS_VISIBLE, TEXT, IS_BINARY, IS_EDITABLE, HIGHLIGHT_LEVEL)
@JsonDeserialize(builder = EduFileBuilder::class)
abstract class EduFileMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(IS_VISIBLE)
  var isVisible: Boolean = true

  lateinit var text: String
    @JsonProperty(TEXT)
    @Encrypt
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    get
    @JsonProperty(TEXT)
    @Encrypt
    set

  var isBinary: Boolean? = null
    @JsonProperty(IS_BINARY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    get
    @JsonProperty(IS_BINARY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    set

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(IS_EDITABLE)
  var isEditable: Boolean = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = HighlightLevelValueFilter::class)
  @JsonProperty(HIGHLIGHT_LEVEL)
  var errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
}

@JsonPropertyOrder(NAME, PLACEHOLDERS, IS_VISIBLE, TEXT, IS_BINARY, IS_EDITABLE, HIGHLIGHT_LEVEL)
@JsonDeserialize(builder = TaskFileBuilder::class)
abstract class TaskFileMixin : EduFileMixin() {
  @JsonProperty(PLACEHOLDERS)
  private lateinit var _answerPlaceholders: List<AnswerPlaceholder>
}

@JsonPropertyOrder(OFFSET, LENGTH, DEPENDENCY, PLACEHOLDER_TEXT)
abstract class AnswerPlaceholderMixin {
  @JsonProperty(OFFSET)
  private var offset: Int = -1

  @JsonProperty(LENGTH)
  private var length: Int = -1

  @JsonProperty(DEPENDENCY)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private lateinit var placeholderDependency: AnswerPlaceholderDependency

  @JsonProperty(PLACEHOLDER_TEXT)
  private lateinit var placeholderText: String
}

@JsonPropertyOrder(OFFSET, LENGTH, DEPENDENCY, POSSIBLE_ANSWER, PLACEHOLDER_TEXT)
abstract class AnswerPlaceholderWithAnswerMixin : AnswerPlaceholderMixin() {
  @JsonProperty(POSSIBLE_ANSWER)
  @Encrypt
  private lateinit var possibleAnswer: String
}

@JsonInclude(JsonInclude.Include.NON_NULL)
abstract class AnswerPlaceholderDependencyMixin {
  @JsonProperty(SECTION)
  private lateinit var sectionName: String

  @JsonProperty(LESSON)
  private lateinit var lessonName: String

  @JsonProperty(TASK)
  private lateinit var taskName: String

  @JsonProperty(FILE)
  private lateinit var fileName: String

  @JsonProperty(PLACEHOLDER)
  private var placeholderIndex: Int = -1

  @JsonProperty(IS_VISIBLE)
  private var isVisible = true
}

class CourseDeserializer : StdDeserializer<Course>(Course::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): Course? {
    val node: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    return deserializeCourse(node, jp.codec)
  }

  private fun deserializeCourse(jsonObject: ObjectNode, codec: ObjectCodec): Course? {
    if (jsonObject.has(COURSE_TYPE)) {
      val courseType = jsonObject.get(COURSE_TYPE).asText()
      val course = codec.treeToValue(jsonObject, EduCourse::class.java)
      if (courseType == MARKETPLACE) {
        course.isMarketplace = true
      }
      return course
    }
    return codec.treeToValue(jsonObject, EduCourse::class.java)
  }
}

class StudyItemDeserializer : StdDeserializer<StudyItem>(StudyItem::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): StudyItem? {
    val node: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    return deserializeItem(node, jp.codec)
  }

  private fun deserializeItem(jsonObject: ObjectNode, codec: ObjectCodec): StudyItem? {
    if (jsonObject.has(TASK_TYPE)) {
      val taskType = jsonObject.get(TASK_TYPE).asText()
      return deserializeTask(jsonObject, taskType, codec)
    }

    return if (!jsonObject.has(ITEM_TYPE)) {
      codec.treeToValue(jsonObject, Lesson::class.java)
    }
    else {
      val itemType = jsonObject.get(ITEM_TYPE).asText()
      when (itemType) {
        LESSON -> codec.treeToValue(jsonObject, Lesson::class.java)
        FRAMEWORK_TYPE -> codec.treeToValue(jsonObject, FrameworkLesson::class.java)
        SECTION -> codec.treeToValue(jsonObject, Section::class.java)
        else -> throw IllegalArgumentException("Unsupported item type: $itemType")
      }
    }
  }
}

fun deserializeTask(node: ObjectNode, taskType: String, objectMapper: ObjectCodec): Task? {
  return when (taskType) {
    IdeTask.IDE_TASK_TYPE -> objectMapper.treeToValue(node, IdeTask::class.java)
    ChoiceTask.CHOICE_TASK_TYPE -> objectMapper.treeToValue(node, ChoiceTask::class.java)
    TheoryTask.THEORY_TASK_TYPE -> objectMapper.treeToValue(node, TheoryTask::class.java)
    CodeTask.CODE_TASK_TYPE -> objectMapper.treeToValue(node, CodeTask::class.java)
    // deprecated: old courses have pycharm tasks
    EduTask.EDU_TASK_TYPE, EduTask.PYCHARM_TASK_TYPE -> {
      objectMapper.treeToValue(node, EduTask::class.java)
    }
    OutputTask.OUTPUT_TASK_TYPE -> objectMapper.treeToValue(node, OutputTask::class.java)
    MatchingTask.MATCHING_TASK_TYPE -> objectMapper.treeToValue(node, MatchingTask::class.java)
    RemoteEduTask.REMOTE_EDU_TASK_TYPE -> objectMapper.treeToValue(node, RemoteEduTask::class.java)
    SortingTask.SORTING_TASK_TYPE -> objectMapper.treeToValue(node, SortingTask::class.java)
    UnsupportedTask.UNSUPPORTED_TASK_TYPE -> objectMapper.treeToValue(node, UnsupportedTask::class.java)
    else -> {
      LOG.warning("Unsupported task type $taskType")
      null
    }
  }
}

@JsonPOJOBuilder(withPrefix = "")
private open class EduFileBuilder {

  private var _name: String = ""
  var name: String
    @JsonProperty(NAME)
    set(value) {
      _name = value
    }
    @JsonProperty(NAME)
    get() = _name

  @JsonProperty(IS_VISIBLE)
  var isVisible: Boolean = true
  @JsonProperty(TEXT)
  @Encrypt
  lateinit var text: String
  @JsonProperty(IS_BINARY)
  var isBinary: Boolean? = null
  @JsonProperty(IS_EDITABLE)
  var isEditable: Boolean = true
  @JsonProperty(HIGHLIGHT_LEVEL)
  var errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
  @JacksonInject(JSON_TEXT_CONVERTER)
  var myJsonTextConverter: JsonTextConverter = ToMemoryTextConverter

  private fun build(): EduFile {
    val result = EduFile()
    updateFile(result)

    return result
  }

  protected fun updateFile(result: EduFile) {
    result.name = name
    result.isVisible = isVisible
    result.isEditable = isEditable
    result.errorHighlightLevel = errorHighlightLevel
    result.contents = myJsonTextConverter.getContents(text, isBinary)
  }
}

@JsonPOJOBuilder(withPrefix = "")
private class TaskFileBuilder : EduFileBuilder() {

  @JsonProperty(PLACEHOLDERS)
  var answerPlaceholders: List<AnswerPlaceholder> = mutableListOf()

  private fun build(): TaskFile {
    val result = TaskFile()
    updateFile(result)

    result.answerPlaceholders = answerPlaceholders
    return result
  }
}
