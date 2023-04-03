package com.jetbrains.edu.learning

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.authorContentsStorage.zip.ZipAuthorContentStorageFactory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentStorageFactory
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.json.configureCourseMapper
import com.jetbrains.edu.learning.json.getCourseMapper
import com.jetbrains.edu.learning.json.migrate
import com.jetbrains.edu.learning.json.mixins.LocalEduCourseMixin
import com.jetbrains.edu.learning.json.readCourseJson
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.zip.ZipFile

object EduUtilsKt {
  @JvmStatic
  @JvmOverloads
  fun DataContext.showPopup(htmlContent: String, position: Balloon.Position = Balloon.Position.above) {
    val balloon = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        htmlContent,
        null,
        UIUtil.getToolTipActionBackground(),
        EduBrowserHyperlinkListener.INSTANCE
      )
      .createBalloon()

    val tooltipRelativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(this)
    balloon.show(tooltipRelativePoint, position)
  }

  @JvmStatic
  fun convertToHtml(markdownText: String): String {
    // Markdown parser is supposed to work with normalized text from document
    val normalizedText = StringUtil.convertLineSeparators(markdownText)

    // org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor considers links starting
    // with "^(vbscript|javascript|file|data):" unsafe and converts them into "#"
    // if `useSafeLinks` is `true`
    val flavour = GFMFlavourDescriptor(useSafeLinks = false)
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownText)

    return HtmlGenerator(normalizedText, parsedTree, flavour, false).generateHtml()
  }

  fun getCourseraCourse(zipFilePath: String): Course? {
    return getLocalCourse(zipFilePath, ::readCourseraCourseJson)
  }

  fun getLocalCourse(zipFilePath: String, readCourseJson: (String, AuthorContentStorageFactory<*>) -> Course? = ::readCourseJson): Course? {
    try {
      val zipFile = ZipFile(zipFilePath)
      val authorContentStorageFactory = ZipAuthorContentStorageFactory()
      val course = zipFile.use {
        val entry = it.getEntry(EduNames.COURSE_META_FILE) ?: return null
        val jsonText = String(it.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8)
        readCourseJson(jsonText, authorContentStorageFactory)
      }
      authorContentStorageFactory.build()
      return course
    }
    catch (e: IOException) {
      LOG.error("Failed to unzip course archive", e)
    }
    return null
  }

  private val LOG = logger<EduUtilsKt>()
}

private fun readCourseraCourseJson(jsonText: String, authorContentStorageFactory: AuthorContentStorageFactory<*>): Course? {
  return try {
    val courseMapper = getCourseMapper(authorContentStorageFactory)
    courseMapper.addMixIn(CourseraCourse::class.java, CourseraCourseMixin::class.java)
    courseMapper.configureCourseMapper(false)
    var courseNode = courseMapper.readTree(jsonText) as ObjectNode
    courseNode = migrate(courseNode)
    courseMapper.treeToValue<CourseraCourse?>(courseNode)
  }
  catch (e: IOException) {
    null
  }
}

@JsonAutoDetect(setterVisibility = JsonAutoDetect.Visibility.NONE)
abstract class CourseraCourseMixin : LocalEduCourseMixin() {
  @Suppress("unused")
  @JsonProperty(YamlMixinNames.SUBMIT_MANUALLY)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  var submitManually = false
}

class NumericInputValidator(val emptyInputMessage: String, val notNumericMessage: String) : InputValidatorEx {
  override fun getErrorText(inputString: String): String? {
    val input = inputString.trim()
    return when {
      input.isEmpty() -> emptyInputMessage
      !isNumeric(input) -> notNumericMessage
      else -> null
    }
  }

  override fun checkInput(inputString: String): Boolean {
    return getErrorText(inputString) == null
  }

  override fun canClose(inputString: String): Boolean = true

  private fun isNumeric(string: String): Boolean {
    return string.all { StringUtil.isDecimalDigit(it) }
  }
}

object Executor {
  fun <T> execCancelable(message: String, callable: Callable<T>): T =
    ProgressManager.getInstance().runProcessWithProgressSynchronously<T, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        EduUtils.execCancelable(callable)
      },
      message, true, null
    )
}

