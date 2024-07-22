@file:JvmName("TaskDescriptionUtil")

package com.jetbrains.edu.learning.taskToolWindow

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.LinkLabel
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.ui.ExperimentalUI
import com.intellij.ui.JBColor
import com.intellij.ui.components.AnActionLink
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isSwing
import com.jetbrains.edu.learning.actions.OpenTaskOnSiteAction
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.SOURCE
import com.jetbrains.edu.learning.taskToolWindow.ui.LightColoredActionLink
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import javax.swing.JPanel

private const val SHORTCUT = "shortcut"
private const val SHORTCUT_ENTITY = "&$SHORTCUT:"
private const val SHORTCUT_ENTITY_ENCODED = "&amp;$SHORTCUT:"
private const val VIDEO_TAG = "video"
private const val IFRAME_TAG = "iframe"
private const val YOUTUBE_VIDEO_ID_LENGTH = 11
const val A_TAG = "a"
const val CODE_TAG = "code"
const val IMG_TAG = "img"
const val SCRIPT_TAG = "script"
const val SRC_ATTRIBUTE = "src"
private const val SPAN_ATTRIBUTE = "span"
private const val HEIGHT_ATTRIBUTE = "height"
const val HREF_ATTRIBUTE = "href"
private const val STYLE_ATTRIBUTE = "style"
private const val CLASS_ATTRIBUTE = "class"
const val TERM_CLASS = "term"
private const val SRCSET_ATTRIBUTE = "srcset"
private const val DARK_SRC_CUSTOM_ATTRIBUTE = "dark-src"
private const val WIDTH_ATTRIBUTE = "width"
private const val BORDER_ATTRIBUTE = "border"
private const val DARK_SUFFIX = "_dark"
private val LOG: Logger = Logger.getInstance("com.jetbrains.edu.learning.taskToolWindow.utils")
private val HYPERSKILL_TAGS = tagsToRegex({ "\\[$it](.*)\\[/$it]" }, "HINT", "PRE", "META") +
                              tagsToRegex({ "\\[$it-\\w+](.*)\\[/$it]" }, "ALERT")
private val YOUTUBE_LINKS_REGEX = "https?://(www\\.)?(youtu\\.be|youtube\\.com)/?(watch\\?v=|embed)?.*".toRegex()
private val EXTERNAL_LINK_REGEX = "https?://.*".toRegex()

private fun tagsToRegex(pattern: (String) -> String, vararg tags: String): List<Regex> = tags.map { pattern(it).toRegex() }

// see EDU-2444
fun removeHyperskillTags(text: StringBuffer) {
  var result: String = text.toString()
  for (regex in HYPERSKILL_TAGS) {
    result = result.replace(regex) { it.groupValues[1] }
  }

  text.delete(0, text.length)
  text.append(result)
}

fun replaceActionIDsWithShortcuts(text: StringBuffer) {
  var lastIndex = 0
  while (lastIndex < text.length) {
    lastIndex = text.indexOf(SHORTCUT_ENTITY, lastIndex)
    var shortcutEntityLength = SHORTCUT_ENTITY.length
    if (lastIndex < 0) {
      //`&` symbol might be replaced with `&amp;`
      lastIndex = text.indexOf(SHORTCUT_ENTITY_ENCODED)
      if (lastIndex < 0) {
        return
      }
      shortcutEntityLength = SHORTCUT_ENTITY_ENCODED.length
    }
    val actionIdStart = lastIndex + shortcutEntityLength
    val actionIdEnd = text.indexOf(";", actionIdStart)
    if (actionIdEnd < 0) {
      return
    }
    val actionId = text.substring(actionIdStart, actionIdEnd)
    var shortcutText = KeymapUtil.getFirstKeyboardShortcutText(actionId)
    if (shortcutText.isEmpty()) {
      shortcutText = "<no shortcut for action $actionId>"
    }
    text.replace(lastIndex, actionIdEnd + 1, shortcutText)
    lastIndex += shortcutText.length
  }
}

fun processYoutubeLink(text: String, taskId: Int): String {
  val document = Jsoup.parse(text)
  val videoElements = document.getElementsByTag(VIDEO_TAG)
  for (element in videoElements) {
    val sourceElements = element.getElementsByTag(SOURCE)
    if (sourceElements.size != 1) {
      LOG.warn("Incorrect number of youtube video sources for task ${taskId}")
      continue
    }
    val src = sourceElements.attr(SRC_ATTRIBUTE)
    val elementToReplaceWith = getClickableImageElement(src, taskId) ?: continue
    element.replaceWith(elementToReplaceWith)
  }
  val iframeElements = document.getElementsByTag(IFRAME_TAG)
  for (element in iframeElements) {
    val src = element.attr(SRC_ATTRIBUTE)
    val elementToReplace = getClickableImageElement(src, taskId) ?: continue
    element.replaceWith(elementToReplace)
  }
  return document.outerHtml()
}

private fun getClickableImageElement(src: String, taskId: Int): Element? {
  val youtubeVideoId = src.getYoutubeVideoId()
  if (youtubeVideoId == null) {
    LOG.warn("Incorrect youtube video link $src for task $taskId")
    return null
  }
  val textToReplace = "<a href=http://www.youtube.com/watch?v=${youtubeVideoId}><img src=http://img.youtube.com/vi/${youtubeVideoId}/0.jpg></a>"
  val documentToReplace = Jsoup.parse(textToReplace)
  val elements = documentToReplace.getElementsByTag("a")
  return if (elements.isNotEmpty()) {
    elements[0]
  }
  else {
    null
  }
}

fun String.getYoutubeVideoId(): String? {
  if (!YOUTUBE_LINKS_REGEX.matches(this)) {
    return null
  }
  val splitLink = this.split("?v=", "/embed/", ".be/", "&", "?")
  return if (splitLink.size >= 2) {
    val id = splitLink[1]
    if (id.length == YOUTUBE_VIDEO_ID_LENGTH) id else null
  }
  else {
    null
  }
}

/**
 * This method replaces the `src` attributes for `<img>` and for `<iframe>` elements.
 * It does it only if the dark theme is currently used.
 * The value to substitute for the original src value is searched in the following steps:
 * 1. If the custom attribute `darkSrc` is set (written as `data-dark-src` inside HTML),
 * then its value is used. No further steps are done.
 * 2. For image (`<img>`), if the `srcset` attribute is set, then its value is used, no further steps are done.
 * 3. For image (`<img>`), if the link directs to a local files, for example `image.png`, and the file `image_dark.png`
 * is also present, then the link is updated to direct to this file with a `_dark` postfix.
 *
 * Note, that the `srcset` attribute is always removed, independently of whether the dark theme is used or not.
 */
fun replaceMediaForTheme(project: Project, task: Task, taskText: Document): Document {
  val isDarkTheme = !JBColor.isBright()

  val imageElements = taskText.getElementsByTag(IMG_TAG)
  for (element in imageElements) {
    updateImageElementAccordingToUiTheme(element, isDarkTheme, task, project)
  }

  if (isDarkTheme) {
    val iframeElements = taskText.getElementsByTag(IFRAME_TAG)
    for (element in iframeElements) {
      useDarkSrcCustomAttributeIfPresent(element)
    }
  }

  return taskText
}

private fun updateImageElementAccordingToUiTheme(element: Element, isDarkTheme: Boolean, task: Task, project: Project) {
  val darkImageSrc = element.getDarkImageSrc(project, task)
  // remove srcset attribute independently of the theme
  element.removeAttr(SRCSET_ATTRIBUTE)

  if (isDarkTheme && darkImageSrc != null) {
    element.attr(SRC_ATTRIBUTE, darkImageSrc)
  }
}

/**
 * Returns an image source supposed to be used with dark theme
 * or null if the corresponding source is not provided
 */
fun Element.getDarkImageSrc(project: Project, task: Task): String? {
  require(tagName() == IMG_TAG) {
    "Element should have `$IMG_TAG`. Found `${tagName()}`"
  }
  // first, try looking at data-dark-src attribute
  val darkSrc = dataset()[DARK_SRC_CUSTOM_ATTRIBUTE]
  if (!darkSrc.isNullOrEmpty()) {
    return darkSrc
  }

  // second, try looking at srcset attribute
  val srcsetValue = attr(SRCSET_ATTRIBUTE)
  if (srcsetValue.isNotEmpty()) {
    return srcsetValue
  }

  // third, try to find a local image file with the _dark postfix
  val srcAttr = attr(SRC_ATTRIBUTE)
  val fileNameWithoutExtension = FileUtilRt.getNameWithoutExtension(srcAttr)
  val fileExtension = FileUtilRt.getExtension(srcAttr)
  val darkImagePath = "$fileNameWithoutExtension$DARK_SUFFIX.$fileExtension"
  val taskDir = task.getDir(project.courseDir)
  // There is no guarantee that `darkImagePath` is a valid local relative path.
  // So `findFileSafe` is used instead of `findFile` not to fail with exception in the case of invalid path,
  // for example, http url or path to a directory
  if (darkImagePath in task.taskFiles || taskDir?.findFileSafe(darkImagePath)?.exists() == true) {
    return darkImagePath
  }

  return null
}

/**
 * The same as [findFile] but returns `null` in case of an invalid path instead of throwing exception
 */
private fun VirtualFile.findFileSafe(relativePath: String): VirtualFile? {
  return runCatching { findFile(relativePath) }.getOrNull()
}

/**
 * Take the value for the `src` attribute from the `dataSrc` attribute (written as `data-dark-src` in HTML),
 * if the latter is present.
 * @return whether the replacement was made
 */
fun useDarkSrcCustomAttributeIfPresent(element: Element): Boolean {
  val darkSrc = element.dataset()[DARK_SRC_CUSTOM_ATTRIBUTE]

  return if (darkSrc != null) {
    element.attr(SRC_ATTRIBUTE, darkSrc)
    true
  }
  else {
    false
  }
}

fun getDashedUnderlineElement(document: Document, text: String): Element =
  document.createElement(SPAN_ATTRIBUTE).apply {
    attr(STYLE_ATTRIBUTE, "border-bottom: 1px dashed gray;")
    attr(CLASS_ATTRIBUTE, TERM_CLASS)
    appendText(text)
  }

fun getPictureSize(fontSize: Int): String {
  return if (isSwing()) {
    fontSize
  }
  else {
    // rounding it to int is needed here, because if we are passing a float number, an arrow disappears in studio
    (fontSize * 1.2).toInt()
  }.toString()
}

fun addActionLinks(course: Course?, linkPanel: JPanel, topMargin: Int, leftMargin: Int) {
  if (course is HyperskillCourse) {
    linkPanel.add(
      createActionLink(EduCoreBundle.message("action.open.on.text", EduNames.JBA), OpenTaskOnSiteAction.ACTION_ID, topMargin, leftMargin)
    )
  }
}

fun createActionLink(
  @LinkLabel actionText: String,
  actionId: String,
  top: Int = 9,
  left: Int = 10
): AnActionLink {
  val link = LightColoredActionLink(actionText, ActionManager.getInstance().getAction(actionId), isExternal = true)
  link.border = JBUI.Borders.empty(top, left, 0, 0)
  return link
}

fun String.containsYoutubeLink(): Boolean = contains(YOUTUBE_LINKS_REGEX)

fun String.replaceEncodedShortcuts() = this.replace(SHORTCUT_ENTITY_ENCODED, SHORTCUT_ENTITY)

fun String.toShortcut(): String = "${SHORTCUT_ENTITY}$this;"

fun String.containsShortcut(): Boolean = startsWith(SHORTCUT_ENTITY) || startsWith(SHORTCUT_ENTITY_ENCODED)

fun link(url: String, text: String, right: Boolean = false): String = """<a${if (right) " class=right " else " "}href="$url">$text</a>"""

@Suppress("UnstableApiUsage")
fun isNewUI(): Boolean = ExperimentalUI.isNewUI()