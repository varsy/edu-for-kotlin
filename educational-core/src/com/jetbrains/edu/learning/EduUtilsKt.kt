package com.jetbrains.edu.learning

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidatorEx
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.PlatformUtils
import com.intellij.util.TimeoutUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.authorContentsStorage.sqlite.SQLiteAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.json.configureCourseMapper
import com.jetbrains.edu.learning.json.getCourseMapper
import com.jetbrains.edu.learning.json.migrate
import com.jetbrains.edu.learning.json.mixins.LocalEduCourseMixin
import com.jetbrains.edu.learning.json.readCourseJson
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.projectView.ProgressUtil.updateCourseProgress
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.zip.ZipFile

object EduUtilsKt {
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

  fun isZip(fileName: String): Boolean = StringUtil.endsWithIgnoreCase(fileName, ".zip")

  @Suppress("UnstableApiUsage")
  fun isAndroidStudio(): Boolean = "AndroidStudio" == PlatformUtils.getPlatformPrefix()

  fun isTaskDescriptionFile(fileName: String): Boolean = TASK_HTML == fileName || TASK_MD == fileName

  @JvmStatic
  fun updateToolWindows(project: Project) {
    TaskDescriptionView.getInstance(project).updateTaskDescription()
    updateCourseProgress(project)
  }

  fun synchronize() {
    FileDocumentManager.getInstance().saveAllDocuments()
    SaveAndSyncHandler.getInstance().refreshOpenFiles()
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
  }

  fun isTestsFile(task: Task, path: String): Boolean {
    val configurator = task.course.configurator ?: return false
    return configurator.isTestFile(task, path)
  }

  fun getCourseraCourse(zipFilePath: String): Course? {
    return getLocalCourse(zipFilePath, ::readCourseraCourseJson)
  }

  fun getLocalCourse(zipFilePath: String, readCourseJson: (String, AuthorContentsStorage) -> Course? = ::readCourseJson): Course? {
    try {
      val zipFile = ZipFile(zipFilePath)
      val authorContentStorage = SQLiteAuthorContentsStorage.openTemporaryDB()
      val course = zipFile.use {
        val entry = it.getEntry(EduNames.COURSE_META_FILE) ?: return null
        val jsonText = String(it.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8)
        readCourseJson(jsonText, authorContentStorage)
      }
      return course
    }
    catch (e: IOException) {
      LOG.error("Failed to unzip course archive", e)
    }
    return null
  }

  fun Project.isNewlyCreated(): Boolean {
    val userData = getUserData(CourseProjectGenerator.EDU_PROJECT_CREATED)
    return userData != null && userData
  }

  fun getCourseModeForNewlyCreatedProject(project: Project): CourseMode? {
    if (project.isDefault || LightEdit.owns(project)) return null
    return project.courseDir.getUserData(CourseProjectGenerator.COURSE_MODE_TO_CREATE)
  }

  @JvmStatic
  fun Project.isEduProject(): Boolean =
    StudyTaskManager.getInstance(this).course != null || getCourseModeForNewlyCreatedProject(this) != null

  fun Project.isStudentProject(): Boolean {
    val course = StudyTaskManager.getInstance(this).course
    return if (course != null && course.isStudy) {
      true
    }
    else CourseMode.STUDENT == getCourseModeForNewlyCreatedProject(this)
  }

  @JvmStatic
  fun replaceAnswerPlaceholder(
    document: Document,
    answerPlaceholder: AnswerPlaceholder
  ) {
    CommandProcessor.getInstance().runUndoTransparentAction {
      ApplicationManager.getApplication().runWriteAction {
        document.replaceString(answerPlaceholder.offset, answerPlaceholder.endOffset, answerPlaceholder.placeholderText)
        FileDocumentManager.getInstance().saveDocument(document)
      }
    }
  }

  // supposed to be called under progress
  fun <T> execCancelable(callable: Callable<T>): T? {
    val future = ApplicationManager.getApplication().executeOnPooledThread(callable)
    while (!future.isCancelled && !future.isDone) {
      ProgressManager.checkCanceled()
      TimeoutUtil.sleep(500)
    }
    var result: T? = null
    try {
      result = future.get()
    }
    catch (e: InterruptedException) {
      LOG.warn(e.message)
    }
    catch (e: ExecutionException) {
      LOG.warn(e.message)
    }
    return result
  }

  private val LOG = logger<EduUtilsKt>()
}

private fun readCourseraCourseJson(jsonText: String, authorContentsStorage: AuthorContentsStorage): Course? {
  return try {
    val courseMapper = getCourseMapper(authorContentsStorage)
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

class NumericInputValidator(private val emptyInputMessage: String, private val notNumericMessage: String) : InputValidatorEx {
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
        EduUtilsKt.execCancelable(callable)
      },
      message, true, null
    )
}
