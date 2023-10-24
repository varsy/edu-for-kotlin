package com.jetbrains.edu.learning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.VERSION
import junit.framework.TestCase
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.test.assertIs

class FileContentsDeserializationTest : EduTestCase() {

  fun `test reading file contents from course_dot_json version 16`() {
    // all file contents should be read as undefined

    val course = createCourseFromJson("testData/fileContents/course archive 16.json", CourseMode.STUDENT)

    val allEduFiles = course.allTasks[0].taskFiles.values + course.additionalFiles
    for (eduFile in allEduFiles) {
      assertIs<UndeterminedContents>(eduFile.contents, "all read file contents must be undetermined because is_binary field is absent")
    }
  }

  fun `test reading file contents from course_dot_json version 17`() {
    // all file contents should be read as either binary or textual

    val course = createCourseFromJson("testData/fileContents/course archive 17.json", CourseMode.STUDENT)

    val allEduFiles = course.allTasks[0].taskFiles.values + course.additionalFiles

    val textualExtensions = setOf("txt", "asdf", "json")

    for (eduFile in allEduFiles) {
      val extension = Path.of(eduFile.name).extension
      if (extension in textualExtensions) {
        assertIs<TextualContents>(eduFile.contents, "${eduFile.name} must have textual contents")
      }
      else {
        assertIs<BinaryContents>(eduFile.contents, "${eduFile.name} must have binary contents")
      }
    }
  }

  fun `test jackson deserialization`() {
    val longText = StringBuilder()
    for (i in 1..100_000)
      longText.append("a")
    val json = """{
      |"text1": "$longText x",
      |"text2": "$longText y",
      |"text3": "$longText z",
      |"text4": "$longText t",
      |"text5": "$longText p",
      |"version": 14,
      |"course_type": "marketplace",
      |"sub_array": [10, 20, 30],
      |"sub_object": {
      |  "version": 19,
      |  "course_type": "moodle"
      |}
      |}""".trimMargin()

    for (i in 1..100) {
      TestCase.assertEquals("marketplace14", getWithReducedMapper(json))
      TestCase.assertEquals("marketplace14", getWithReadTree(json))
      TestCase.assertEquals("marketplace14", getWithStreamingApi(json))
    }
  }

  private fun getWithReadTree(jsonText: String): String {

    val tree = ObjectMapper().readTree(jsonText)

    val courseType = tree.get("course_type").asText()
    val version = tree.get("version").asInt()

    return "${courseType}${version}"
  }

  private fun getWithReducedMapper(jsonText: String): String {
    // We are going to read course.json, but we need only the "version", and the "course_type" fields
    class CourseJson(
      @JsonProperty(VERSION)
      val version: Int?,
      @JsonProperty(COURSE_TYPE)
      val courseType: String?
    )

    val reducedMapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val courseJson = reducedMapper.readValue(jsonText, CourseJson::class.java)

    return "${courseJson.courseType}${courseJson.version}"
  }

  private fun getWithStreamingApi(jsonText: String): String = JsonFactory().createParser(jsonText).use { parser ->

    var version: Int? = null
    var courseType: String? = null

    // read start object token
    parser.nextToken()
    if (!parser.hasToken(JsonToken.START_OBJECT)) return ""

    // read object fields until the END_OBJECT
    while (parser.nextToken() != null && !parser.hasToken(JsonToken.END_OBJECT)) {
      if (!parser.hasToken(JsonToken.FIELD_NAME)) return ""

      when (parser.currentName) {
        VERSION -> version = parser.nextIntValue(0)
        COURSE_TYPE -> courseType = parser.nextTextValue()
        else -> {
          parser.nextToken()
          parser.skipChildren()
        }
      }
    }

    return "$courseType$version"
  }
}