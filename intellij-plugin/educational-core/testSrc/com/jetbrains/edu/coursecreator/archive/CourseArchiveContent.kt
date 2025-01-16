package com.jetbrains.edu.coursecreator.archive

import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.testFramework.core.FileComparisonFailedError
import com.intellij.testFramework.fixtures.BasePlatformTestCase.fail
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_META_FILE
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.isTeamCity
import org.junit.Assert
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

data class CourseArchiveContent(
  val files: Map<String, ByteArray>
) {
  val courseJson: String
    get() = files[COURSE_META_FILE]?.toString(Charsets.UTF_8) ?: error("No `$COURSE_META_FILE` in archive")

  fun assertEquals(
    expected: CourseArchiveContent,
    expectedBaseDir: File,
    generateExtraFiles: Boolean
  ) {
    // files not found in the actual zip archive
    val missingFiles = mutableSetOf<String>()
    // files in the actual zip archive which don't exist in expected files
    val extraFiles = files.toMutableMap()
    val filesToCompare = mutableMapOf<String, ContentsToCompare>()

    for ((path, expectedContent) in expected.files) {
      val actualContent = extraFiles.remove(path)
      if (actualContent == null) {
        missingFiles += path
      }
      else {
        filesToCompare[path] = ContentsToCompare(expected = expectedContent, actual = actualContent)
      }
    }

    // Check it explicitly here to catch a pathological case,
    // when both expected and actual data don't contain `course.json`
    if (files[COURSE_META_FILE] == null) {
      missingFiles += COURSE_META_FILE
    }

    if (missingFiles.isNotEmpty()) {
      fail("Missing files in archive: $missingFiles")
    }

    if (extraFiles.isNotEmpty()) {
      if (generateExtraFiles) {
        for ((path, content) in extraFiles) {
          val file = expectedBaseDir.resolve(path)
          FileUtil.writeToFile(file, content)
        }
      }

      fail("Unexpected files in archive: ${extraFiles.keys}")
    }

    for ((path, contents) in filesToCompare) {
      if (path == COURSE_META_FILE) {
        checkCourseArchiveJson(contents, expectedBaseDir)
      }
      else {
        Assert.assertArrayEquals("Unexpected $path content", contents.expected, contents.actual)
      }
    }
  }

  private fun checkCourseArchiveJson(contents: ContentsToCompare, expectedBaseDir: File) {
    val expectedJson = contents.expected.toString(Charsets.UTF_8).withLatestJsonVersion()
    val actualJson = contents.actual.toString(Charsets.UTF_8)

    if (expectedJson != actualJson) {
      if (!isTeamCity) {
        // `FileComparisonFailedError` is used here instead of `assertEquals`
        // to add the possibility to move changes from actual to expected files using IDE Diff View
        throw FileComparisonFailedError(
          "Unexpected $COURSE_META_FILE content",
          expectedJson,
          actualJson,
          expectedBaseDir.resolve(COURSE_META_FILE).absolutePath
        )
      }
      else {
        // Teamcity doesn't know about `FileComparisonFailedError` and cannot show diff in Web if assert fails.
        // To make it work, let's fall back to `Assert.assertEquals`
        Assert.assertEquals("Unexpected $COURSE_META_FILE content", expectedJson, actualJson)
      }
    }
  }

  /**
   * Replaces `<last version>` in given string with [JSON_FORMAT_VERSION]
   */
  private fun String.withLatestJsonVersion(): String = replace(LAST_VERSION_REGEX, """"version" : $JSON_FORMAT_VERSION""")

  companion object {
    private val LAST_VERSION_REGEX = """"version"\s*:\s*<last version>""".toRegex()

    fun fromBytes(data: ByteArray): CourseArchiveContent {
      val files = mutableMapOf<String, ByteArray>()
      ZipInputStream(ByteArrayInputStream(data)).use { zipIn ->
        while (true) {
          val entry = zipIn.nextEntry ?: break
          files[entry.name] = zipIn.readAllBytes()
        }
      }

      return CourseArchiveContent(files)
    }
  }

  private class ContentsToCompare(val expected: ByteArray, val actual: ByteArray)
}
