@file:JvmName("VirtualFileExt")

package com.jetbrains.edu.learning

import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.concurrency.annotations.RequiresWriteLock
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.actions.BinaryContentsFromDisk
import com.jetbrains.edu.coursecreator.actions.CourseArchiveIndicator
import com.jetbrains.edu.learning.EduDocumentListener.Companion.runWithListener
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroUtils
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.apache.commons.codec.binary.Base64
import java.io.IOException

fun VirtualFile.getEditor(project: Project): Editor? {
  val selectedEditor = invokeAndWaitIfNeeded { FileEditorManager.getInstance(project).getSelectedEditor(this) }
  return if (selectedEditor is TextEditor) selectedEditor.editor else null
}

val VirtualFile.document
  get() : Document = FileDocumentManager.getInstance().getDocument(this) ?: error("Cannot find document for a file: ${name}")

fun VirtualFile.startLoading(project: Project) {
  val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(this) ?: return

  fileEditor.setViewer(true)

  fileEditor.loadingPanel?.apply {
    setLoadingText(EduCoreBundle.message("editor.loading.solution"))
    startLoading()
  }
}

fun VirtualFile.stopLoading(project: Project) {
  val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(this) ?: return

  fileEditor.setViewer(false)
  fileEditor.loadingPanel?.stopLoading()
}

private fun FileEditor.setViewer(isViewer: Boolean) {
  val textEditor = this as? TextEditor ?: return
  (textEditor.editor as EditorEx).isViewer = isViewer
}

private val FileEditor.loadingPanel: JBLoadingPanel?
  get() = UIUtil.findComponentOfType(component, JBLoadingPanel::class.java)

fun VirtualFile.getSection(project: Project): Section? {
  return getSection(project.toCourseInfoHolder())
}

fun VirtualFile.getSection(holder: CourseInfoHolder<out Course?>): Section? {
  val course = holder.course ?: return null
  if (!isDirectory) return null
  return if (holder.courseDir == parent) course.getSection(name) else null
}

fun VirtualFile.isSectionDirectory(project: Project): Boolean {
  return getSection(project) != null
}

fun VirtualFile.getLesson(project: Project): Lesson? {
  return getLesson(project.toCourseInfoHolder())
}

fun VirtualFile.getLesson(holder: CourseInfoHolder<out Course?>): Lesson? {
  val course = holder.course ?: return null
  if (!isDirectory) return null
  if (parent == null) return null

  val section = parent.getSection(holder)
  if (section != null) {
    return section.getLesson(name)
  }
  return if (holder.courseDir == parent) course.getLesson(name) else null
}

fun VirtualFile.isLessonDirectory(project: Project): Boolean {
  return getLesson(project) != null
}

fun VirtualFile.getContainingTask(project: Project): Task? {
  return getContainingTask(project.toCourseInfoHolder())
}

fun VirtualFile.getContainingTask(holder: CourseInfoHolder<out Course?>): Task? {
  val course = holder.course ?: return null
  val taskDir = getTaskDir(holder) ?: return null
  val lessonDir = taskDir.parent ?: return null
  val lesson = lessonDir.getLesson(holder) ?: return null
  return if (lesson is FrameworkLesson && course.isStudy) {
    lesson.currentTask()
  }
  else {
    lesson.getTask(taskDir.name)
  }
}

fun VirtualFile.getTask(project: Project): Task? {
  return getTask(project.toCourseInfoHolder())
}

fun VirtualFile.getTask(holder: CourseInfoHolder<out Course?>): Task? {
  if (!isDirectory) return null
  val lesson: Lesson = parent?.getLesson(holder) ?: return null
  return lesson.getTask(name)
}

fun VirtualFile.isTaskDirectory(project: Project): Boolean {
  return getTask(project) != null
}

fun VirtualFile.getTextFromTaskTextFile(): String? {
  val document = FileDocumentManager.getInstance().getDocument(this)
  return document?.text
}

fun VirtualFile.getStudyItem(project: Project): StudyItem? {
  val course = project.course ?: return null
  val courseDir = project.courseDir
  if (courseDir == this) return course
  val section = getSection(project)
  if (section != null) return section
  val lesson = getLesson(project)
  return lesson ?: getTask(project)
}

/**
 * @return true, if file doesn't belong to task (in terms of course structure)
 * but can be added to it as task, test or additional file.
 * Otherwise, returns false
 */
fun VirtualFile.canBeAddedToTask(project: Project): Boolean {
  if (isDirectory) return false
  return if (isTaskSpecialFile()) false else !belongsToTask(project)
}

/**
 * @return true, if some task contains given `file` as task, test or additional file.
 * Otherwise, returns false
 */
fun VirtualFile.belongsToTask(project: Project): Boolean {
  val task = getContainingTask(project) ?: return false
  val relativePath = pathRelativeToTask(project)
  return task.getTaskFile(relativePath) != null
}

fun VirtualFile.canBelongToCourse(project: Project): Boolean {
  if (isSectionDirectory(project) || isLessonDirectory(project) || isTaskDirectory(project)) return true
  return if (isDirectory) {
    getContainingTask(project) != null
  }
  else {
    belongsToTask(project)
  }
}

fun VirtualFile.pathRelativeToTask(project: Project): String {
  return pathRelativeToTask(project.toCourseInfoHolder())
}

fun VirtualFile.pathRelativeToTask(holder: CourseInfoHolder<out Course?>): String {
  val taskDir = getTaskDir(holder) ?: return name
  return FileUtil.getRelativePath(taskDir.path, path, VfsUtilCore.VFS_SEPARATOR_CHAR) ?: return name
}

fun VirtualFile.getTaskDir(project: Project): VirtualFile? {
  return getTaskDir(project.toCourseInfoHolder())
}

fun VirtualFile.getTaskDir(holder: CourseInfoHolder<out Course?>): VirtualFile? {
  var taskDir = this
  while (true) {
    val lessonDirCandidate = taskDir.parent ?: return null
    val lesson = lessonDirCandidate.getLesson(holder)
    if (lesson != null) {
      if (lesson is FrameworkLesson && TASK == taskDir.name || lesson.getTask(taskDir.name) != null) {
        return taskDir
      }
    }
    taskDir = lessonDirCandidate
  }
}

fun VirtualFile.isTestsFile(project: Project): Boolean {
  if (isDirectory) return false
  val task = getContainingTask(project) ?: return false
  val path: String = pathRelativeToTask(project)
  val course = StudyTaskManager.getInstance(project).course ?: return false
  val configurator = course.configurator ?: return false
  return configurator.isTestFile(task, path)
}

fun VirtualFile.isTaskRunConfigurationFile(project: Project): Boolean {
  return isTaskRunConfigurationFile(project.toCourseInfoHolder())
}

fun VirtualFile.isTaskRunConfigurationFile(holder: CourseInfoHolder<out Course?>): Boolean {
  if (isDirectory) return false
  val parent = parent ?: return false
  if (parent.name != EduNames.RUN_CONFIGURATION_DIR) return false
  val grandParent = parent.parent
  return grandParent != null && grandParent.getTaskDir(holder) == grandParent
}

/**
 * It's supposed to be used to associate [TaskFile] with virtual file which is not located on disk,
 * for example, [LightVirtualFile]
 */
val TASK_FILE: Key<TaskFile> = Key.create("TASK_FILE")

fun VirtualFile.getTaskFile(project: Project): TaskFile? {
  val taskFile = getUserData(TASK_FILE)
  if (taskFile != null) return taskFile

  return getTaskFile(project.toCourseInfoHolder())
}

fun VirtualFile.getTaskFile(holder: CourseInfoHolder<out Course?>): TaskFile? {
  val task = getContainingTask(holder)
  return task?.getTaskFile(pathRelativeToTask(holder))
}

val VirtualFile.isToEncodeContent: Boolean
  get(): Boolean = toEncodeFileContent(this)

@Throws(IOException::class)
fun VirtualFile.loadEncodedContent(isToEncodeContent: Boolean = this.isToEncodeContent): String {
  return if (isToEncodeContent) {
    Base64.encodeBase64String(contentsToByteArray())
  }
  else {
    VfsUtilCore.loadText(this)
  }
}

@Throws(HugeBinaryFileException::class)
fun VirtualFile.toStudentFile(project: Project, task: Task, indicator: CourseArchiveIndicator? = null): TaskFile? {
  try {
    val taskCopy = task.copy()
    val taskFile = taskCopy.getTaskFile(pathRelativeToTask(project)) ?: return null
    if (isToEncodeContent) {
      if (task.lesson is FrameworkLesson && length >= getBinaryFileLimit()) {
        throw HugeBinaryFileException("${task.getPathInCourse()}/${taskFile.name}", length, getBinaryFileLimit().toLong(), true)
      }
      taskFile.contents = BinaryContentsFromDisk(this, indicator)
      return taskFile
    }
    FileDocumentManager.getInstance().saveDocument(document)
    val studentFile = LightVirtualFile("student_task", PlainTextFileType.INSTANCE, document.text)
    runWithListener(project, taskFile, studentFile) { studentDocument: Document ->
      for (placeholder in taskFile.answerPlaceholders) {
        try {
          placeholder.possibleAnswer = studentDocument.getText(TextRange.create(placeholder.offset, placeholder.endOffset))
          EduUtilsKt.replaceAnswerPlaceholder(studentDocument, placeholder)
        }
        catch (e: IndexOutOfBoundsException) {
          // We are here because placeholder is broken. We need to put broken placeholder into exception.
          // We need to take it from original task, because taskCopy has issues with links (taskCopy.lesson is always null)
          val file = task.getTaskFile(taskFile.name)
          val answerPlaceholder = file?.answerPlaceholders?.get(placeholder.index)
          throw BrokenPlaceholderException(EduCoreBundle.message("exception.broken.placeholder.title"), answerPlaceholder ?: placeholder)
        }
      }
      val text = studentDocument.immutableCharSequence.toString()
      // We have just substituted placeholders and thus changed the text for the student file.
      // It means we can not use the text from disk, and we should store the result in memory.
      // We could not substitute placeholders later, because during placeholder substitution we compute
      // their positions and store them inside the TaskFile.
      val inMemoryText = EduMacroUtils.collapseMacrosForFile(project.toCourseInfoHolder(), this, text)

      taskFile.contents = object: TextualContents {
        override val text: String
          get() {
            indicator?.readFile(this@toStudentFile)
            return inMemoryText
          }
      }
    }
    return taskFile
  }
  catch (e: IOException) {
    LOG.error("Failed to convert `${path}` to student file")
  }
  return null
}

fun VirtualFile.setHighlightLevel(project: Project, highlightLevel: EduFileErrorHighlightLevel) {
  runInEdt {
    runWriteAction {
      setHighlightLevelInsideWriteAction(project, highlightLevel)
    }
  }
}

fun VirtualFile.setHighlightLevelInsideWriteAction(project: Project, highlightLevel: EduFileErrorHighlightLevel) {
  checkIsWriteActionAllowed()

  // files may disappear for example in framework lessons
  if (!exists())
    return

  val fileHighlightLevel = if (highlightLevel == EduFileErrorHighlightLevel.NONE) {
    FileHighlightingSetting.SKIP_HIGHLIGHTING
  } else {
    FileHighlightingSetting.FORCE_HIGHLIGHTING
  }
  val psiFile = PsiManager.getInstance(project).findFile(this) ?: return
  
  // TriggerCompilerHighlightingService will fail if the document for the virtualFile is null.
  // Read the documentation for FileDocumentManager.getDocument to find out when the document may be null.
  if (FileDocumentManager.getInstance().getDocument(this) == null) return

  HighlightLevelUtil.forceRootHighlighting(psiFile, fileHighlightLevel) // this utility method makes additional null checks
}

@RequiresWriteLock
fun VirtualFile.removeWithEmptyParents(taskDir: VirtualFile) {
  val parent = this.parent
  delete(javaClass)
  if (parent != taskDir && parent.children.isEmpty()) {
    parent.removeWithEmptyParents(taskDir)
  }
}

private val LOG = Logger.getInstance("com.jetbrains.edu.learning.VirtualFileExt")
