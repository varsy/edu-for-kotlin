@file:JvmName("CourseArchiveReader")

package com.jetbrains.edu.learning.json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.MARKETPLACE
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.json.encrypt.getAesKey
import com.jetbrains.edu.learning.json.migration.*
import com.jetbrains.edu.learning.json.mixins.*
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.VERSION
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private val LOG = logger<LocalEduCourseMixin>()

const val AUTHOR_CONTENTS_STORAGE_INJECTION_KEY = "author_contents_storage"

fun readCourseJson(jsonText: String, authorContentsStorage: AuthorContentsStorage): Course? {
  return try {
    val courseMapper = getCourseMapper(authorContentsStorage)
    val isArchiveEncrypted = isArchiveEncrypted(jsonText, courseMapper)
    courseMapper.configureCourseMapper(isArchiveEncrypted)
    var courseNode = courseMapper.readTree(jsonText) as ObjectNode
    courseNode = migrate(courseNode)
    courseMapper.treeToValue(courseNode)
  }
  catch (e: IOException) {
    LOG.severe("Failed to read course json \n" + e.message)
    null
  }
}

private fun isArchiveEncrypted(jsonText: String, courseMapper: ObjectMapper): Boolean {
  val courseNode = courseMapper.readTree(jsonText) as ObjectNode
  val version = courseNode.get(VERSION)?.asInt() ?: error("Format version is null")
  if (version >= 12) return true
  val courseType = courseNode.get(COURSE_TYPE)?.asText()
  return courseType == MARKETPLACE
}

fun migrate(jsonObject: ObjectNode): ObjectNode {
  return migrate(jsonObject, JSON_FORMAT_VERSION)
}

@VisibleForTesting
fun migrate(node: ObjectNode, maxVersion: Int): ObjectNode {
  var jsonObject = node
  val jsonVersion = jsonObject.get(VERSION)
  var version = jsonVersion?.asInt() ?: 1

  while (version < maxVersion) {
    var converter: JsonLocalCourseConverter? = null
    when (version) {
      6 -> converter = ToSeventhVersionLocalCourseConverter()
      7 -> converter = To8VersionLocalCourseConverter()
      8 -> converter = To9VersionLocalCourseConverter()
      9 -> converter = To10VersionLocalCourseConverter()
      10 -> converter = To11VersionLocalCourseConverter()
      11 -> converter = To12VersionLocalCourseConverter()
    }
    if (converter != null) {
      jsonObject = converter.convert(jsonObject)
    }
    version++
  }
  return jsonObject
}

fun getCourseMapper(authorContentsStorage: AuthorContentsStorage): ObjectMapper {
  return JsonMapper.builder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .disable(MapperFeature.AUTO_DETECT_FIELDS)
    .disable(MapperFeature.AUTO_DETECT_GETTERS)
    .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    .disable(MapperFeature.AUTO_DETECT_CREATORS)
    .setDateFormat()
    .injectableValues(InjectableValues.Std(mapOf(AUTHOR_CONTENTS_STORAGE_INJECTION_KEY to authorContentsStorage)))
    .build()
}

fun ObjectMapper.configureCourseMapper(isEncrypted: Boolean) {
  if (isEncrypted) {
    registerModule(EncryptionModule(getAesKey()))
  }
  val module = SimpleModule()
  module.addDeserializer(StudyItem::class.java, StudyItemDeserializer())
  module.addDeserializer(Course::class.java, CourseDeserializer())
  registerModule(module)

  addStudyItemMixins()
}

fun ObjectMapper.addStudyItemMixins() {
  addMixIn(EduCourse::class.java, RemoteEduCourseMixin::class.java)
  addMixIn(PluginInfo::class.java, PluginInfoMixin::class.java)
  addMixIn(Section::class.java, RemoteSectionMixin::class.java)
  addMixIn(FrameworkLesson::class.java, RemoteFrameworkLessonMixin::class.java)
  addMixIn(Lesson::class.java, RemoteLessonMixin::class.java)
  addMixIn(Task::class.java, RemoteTaskMixin::class.java)
  addMixIn(ChoiceTask::class.java, ChoiceTaskLocalMixin::class.java)
  addMixIn(ChoiceOption::class.java, ChoiceOptionLocalMixin::class.java)
  addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
  addMixIn(EduFile::class.java, EduFileMixin::class.java)
  addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
  addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
}

fun JsonMapper.Builder.setDateFormat(): JsonMapper.Builder {
  val mapperDateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
  mapperDateFormat.timeZone = TimeZone.getTimeZone("UTC")
  return defaultDateFormat(mapperDateFormat)
}
