package com.jetbrains.edu.learning.courseGeneration

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.modifyModules
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.io.ReadOnlyAttributeUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.HTML
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.MD
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.LESSON
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SECTION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_HTML
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK_MD
import com.jetbrains.edu.learning.courseFormat.ext.dirName
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.shouldBeEmpty
import com.jetbrains.edu.learning.courseFormat.fileContents.BinaryContents
import com.jetbrains.edu.learning.courseFormat.fileContents.FileContents
import com.jetbrains.edu.learning.courseFormat.fileContents.TextualContents
import com.jetbrains.edu.learning.courseFormat.fileContents.UndeterminedContents
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.*
import com.jetbrains.edu.learning.courseGeneration.macro.EduMacroUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

object GeneratorUtils {

  private val INVALID_SYMBOLS: Regex = """[/\\:<>"?*|;&]""".toRegex()
  private val INVALID_TRAILING_SYMBOLS: CharArray = charArrayOf(' ', '.', '!')

  @Throws(IOException::class)
  @JvmStatic
  fun createCourse(
    holder: CourseInfoHolder<Course>,
    indicator: ProgressIndicator
  ) {
    val course = holder.course
    indicator.isIndeterminate = false

    val items = course.items
    for ((i, item) in items.withIndex()) {
      indicator.fraction = (i + 1).toDouble() / items.size

      if (item is Lesson) {
        indicator.text = EduCoreBundle.message("generate.lesson.progress.text", i + 1, items.size)
        createLesson(holder, item, holder.courseDir)
      }
      else if (item is Section) {
        indicator.text = EduCoreBundle.message("generate.section.progress.text", i + 1, items.size)
        createSection(holder, item, holder.courseDir)
      }
    }
    indicator.text = EduCoreBundle.message("generate.additional.files.progress.text")
    unpackAdditionalFiles(holder, ALL_EXCEPT_IDEA_DIRECTORY)

    EduCounterUsageCollector.studyItemCreated(course)
  }

  @Throws(IOException::class)
  fun createSection(project: Project, item: Section, baseDir: VirtualFile): VirtualFile {
    return createSection(project.toCourseInfoHolder(), item, baseDir)
  }

  @Throws(IOException::class)
  private fun createSection(holder: CourseInfoHolder<out Course?>, item: Section, baseDir: VirtualFile): VirtualFile {
    val sectionDir = createUniqueDir(baseDir, item)

    for (lesson in item.lessons) {
      createLesson(holder, lesson, sectionDir)
    }
    EduCounterUsageCollector.studyItemCreated(item)
    return sectionDir
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createLesson(project: Project, lesson: Lesson, parentDir: VirtualFile): VirtualFile {
    return createLesson(project.toCourseInfoHolder(), lesson, parentDir)
  }

  @Throws(IOException::class)
  @JvmStatic
  private fun createLesson(holder: CourseInfoHolder<out Course?>, lesson: Lesson, parentDir: VirtualFile): VirtualFile {
    val lessonDir = createUniqueDir(parentDir, lesson)
    val taskList = lesson.taskList
    for (task in taskList) {
      createTask(holder, task, lessonDir)
    }
    EduCounterUsageCollector.studyItemCreated(lesson)
    return lessonDir
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createTask(project: Project, task: Task, lessonDir: VirtualFile): VirtualFile {
    return createTask(project.toCourseInfoHolder(), task, lessonDir)
  }

  @Throws(IOException::class)
  @JvmStatic
  private fun createTask(holder: CourseInfoHolder<out Course?>, task: Task, lessonDir: VirtualFile): VirtualFile {
    val isFirstInFrameworkLesson = task.parent is FrameworkLesson && task.index == 1
    val isStudyCourse = task.course.isStudy
    val (contentDir, configDir) = if (isStudyCourse && isFirstInFrameworkLesson) {
      // create config dir for yaml files and task description files
      val configDir = createUniqueDir(lessonDir, task)
      // create content dir with specific for framework lesson task name
      val contentDir = createUniqueDir(lessonDir, task, task.dirName, false)
      contentDir to configDir
    }
    else {
      val taskDir = createUniqueDir(lessonDir, task)
      taskDir to taskDir
    }

    if (!isStudyCourse || task.parent !is FrameworkLesson || isFirstInFrameworkLesson) {
      createTaskContent(holder, task, contentDir)
    }

    createDescriptionFile(holder, configDir, task)
    EduCounterUsageCollector.studyItemCreated(task)
    return contentDir
  }

  @Throws(IOException::class)
  fun createTaskContent(project: Project, task: Task, taskDir: VirtualFile) {
    createTaskContent(project.toCourseInfoHolder(), task, taskDir)
  }

  @Throws(IOException::class)
  private fun createTaskContent(holder: CourseInfoHolder<out Course?>, task: Task, taskDir: VirtualFile) {
    val (testFiles, taskFiles) = task.taskFiles.values.partition { task.shouldBeEmpty(it.name) }

    for (file in taskFiles) {
      createChildFile(holder, taskDir, file.name, file.contents, file.isEditable)
    }

    for (file in testFiles) {
      createChildFile(holder, taskDir, file.name, TextualContents.EMPTY, file.isEditable)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createDescriptionFile(project: Project, taskDir: VirtualFile, task: Task): VirtualFile? {
    return createDescriptionFile(project.toCourseInfoHolder(), taskDir, task)
  }

  @Throws(IOException::class)
  @JvmStatic
  private fun createDescriptionFile(holder: CourseInfoHolder<out Course?>, taskDir: VirtualFile, task: Task): VirtualFile? {
    val descriptionFileName = when (task.descriptionFormat) {
      HTML -> TASK_HTML
      MD -> TASK_MD
    }

    return createChildFile(holder, taskDir, descriptionFileName, TextualContents(task.descriptionText))
  }

  enum class IdeaDirectoryUnpackMode(val insideIdeaDirectory: Boolean, val outsideIdeaDirectory: Boolean) {
    ALL_FILES(true, true),
    ONLY_IDEA_DIRECTORY(false, true),
    ALL_EXCEPT_IDEA_DIRECTORY(true, false)
  }

  @Throws(IOException::class)
  fun unpackAdditionalFiles(holder: CourseInfoHolder<Course>, unpackMode: IdeaDirectoryUnpackMode) {
    val course = holder.course
    for (file in course.additionalFiles) {
      val insideIdeaDirectory = file.name == Project.DIRECTORY_STORE_FOLDER || file.name.startsWith("${Project.DIRECTORY_STORE_FOLDER}/")

      if (insideIdeaDirectory && unpackMode.outsideIdeaDirectory || !insideIdeaDirectory && unpackMode.insideIdeaDirectory) {
        createChildFile(holder, holder.courseDir, file.name, file.contents, file.isEditable)
      }
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  @Deprecated(
    "Use the other createChildFile() method, where you explicitly specify whether text is binary or not",
    ReplaceWith("GeneratorUtils.createChildFile(holder, parentDir, path, fileContent, isEditable)")
  )
  fun createChildFile(project: Project, parentDir: VirtualFile, path: String, text: String): VirtualFile? {
    return createChildFile(project.toCourseInfoHolder(), parentDir, path, UndeterminedContents(text))
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createTextChildFile(project: Project, parentDir: VirtualFile, path: String, text: String): VirtualFile? {
    return createTextChildFile(project.toCourseInfoHolder(), parentDir, path, text)
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createTextChildFile(
    holder: CourseInfoHolder<out Course?>,
    parentDir: VirtualFile,
    path: String,
    text: String,
    isEditable: Boolean = true
  ): VirtualFile? {
    return createChildFile(holder, parentDir, path, TextualContents(text), isEditable)
  }

  @Throws(IOException::class)
  @JvmStatic
  fun createChildFile(
    holder: CourseInfoHolder<out Course?>,
    parentDir: VirtualFile,
    path: String,
    fileContents: FileContents,
    isEditable: Boolean = true
  ): VirtualFile? {
    return runInWriteActionAndWait(ThrowableComputable {
      val file = doCreateChildFile(holder, parentDir, path, fileContents)
      val course = holder.course
      if (course != null && file != null && !isEditable) {
        addNonEditableFileToCourse(course, file)
      }
      file
    })
  }

  @Throws(IOException::class)
  private fun doCreateChildFile(
    holder: CourseInfoHolder<out Course?>,
    parentDir: VirtualFile,
    path: String,
    fileContents: FileContents
  ): VirtualFile? {
    checkIsWriteActionAllowed()

    var newDirectories: String? = null
    var fileName = path
    var dir: VirtualFile? = parentDir
    if (path.contains("/")) {
      val pos = path.lastIndexOf("/")
      fileName = path.substring(pos + 1)
      newDirectories = path.substring(0, pos)
    }
    if (newDirectories != null) {
      dir = VfsUtil.createDirectoryIfMissing(parentDir, newDirectories)
    }

    dir ?: return null

    val virtualTaskFile = dir.findOrCreateChildData(parentDir, fileName)

    fun writeBinary(bytes: ByteArray) {
      virtualTaskFile.setBinaryContent(bytes)
    }

    fun writeTextual(text: String) {
      val expandedText = EduMacroUtils.expandMacrosForFile(holder, virtualTaskFile, text)
      VfsUtil.saveText(virtualTaskFile, expandedText)
    }

    when (fileContents) {
      is BinaryContents -> writeBinary(fileContents.bytes)
      is TextualContents -> writeTextual(fileContents.text)
      is UndeterminedContents -> if (virtualTaskFile.isToEncodeContent) { // fallback to the legacy way to interpret task file content
        writeBinary(fileContents.bytes)
      }
      else {
        writeTextual(fileContents.text)
      }
    }

    return virtualTaskFile
  }

  @Throws(IOException::class)
  fun addNonEditableFileToCourse(course: Course, virtualTaskFile: VirtualFile) {
    checkIsWriteActionAllowed()
    if (course.isStudy) {
      course.addNonEditableFile(virtualTaskFile.path)
      ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualTaskFile, true)
    }
  }

  @Throws(IOException::class)
  fun removeNonEditableFileFromCourse(course: Course, virtualTaskFile: VirtualFile) {
    ApplicationManager.getApplication().assertWriteAccessAllowed()
    if (course.isStudy) {
      course.removeNonEditableFile(virtualTaskFile.path)
      ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualTaskFile, false)
    }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> runInWriteActionAndWait(action: ThrowableComputable<T, IOException>): T {
    val application = ApplicationManager.getApplication()
    val resultRef = AtomicReference<T>()
    val exceptionRef = AtomicReference<IOException>()
    application.invokeAndWait {
      application.runWriteAction {
        try {
          resultRef.set(action.compute())
        }
        catch (e: IOException) {
          exceptionRef.set(e)
        }
      }
    }
    return if (exceptionRef.get() != null) {
      throw IOException(exceptionRef.get())
    }
    else {
      resultRef.get()
    }
  }

  /**
   * Non unique lesson/task/section names can be received from stepik
   */
  @JvmStatic
  fun getUniqueValidName(parentDir: VirtualFile, name: String): String {
    val validName = name.convertToValidName()
    var index = 0
    var candidateName = validName
    while (parentDir.findChild(candidateName) != null) {
      index++
      candidateName = "$validName ($index)"
    }
    return candidateName
  }

  @JvmStatic
  fun String.convertToValidName(): String {
    return replace(INVALID_SYMBOLS, " ").trimEnd(*INVALID_TRAILING_SYMBOLS)
  }

  private fun createUniqueDir(
    parentDir: VirtualFile,
    item: StudyItem,
    baseDirName: String = item.name,
    needUpdateItem: Boolean = true
  ): VirtualFile {
    val uniqueDirName = getUniqueValidName(parentDir, baseDirName)
    if (uniqueDirName != baseDirName && needUpdateItem) {
      item.customPresentableName = item.name
      item.name = uniqueDirName
    }

    return runInWriteActionAndWait(ThrowableComputable {
      VfsUtil.createDirectoryIfMissing(parentDir, uniqueDirName)
    })
  }

  @JvmStatic
  fun joinPaths(prefix: String?, suffix: String): String {
    return if (prefix.isNullOrEmpty()) suffix else "$prefix${VfsUtilCore.VFS_SEPARATOR_CHAR}$suffix"
  }

  @JvmStatic
  fun joinPaths(vararg paths: String): String {
    return paths.filter { it.isNotBlank() }.joinToString(VfsUtilCore.VFS_SEPARATOR_CHAR.toString())
  }

  @JvmStatic
  @JvmOverloads
  fun getInternalTemplateText(templateName: String, templateVariables: Map<String, Any> = emptyMap()): String =
    FileTemplateManager.getDefaultInstance().getInternalTemplate(templateName).getText(templateVariables)

  @JvmStatic
  fun getJ2eeTemplateText(templateName: String): String =
    FileTemplateManager.getDefaultInstance().getJ2eeTemplate(templateName).text

  @Throws(IOException::class)
  fun evaluateExistingTemplate(child: VirtualFile, templateVariables: Map<String, Any>) {
    val rawContent = VfsUtil.loadText(child)
    val content = FileTemplateUtil.mergeTemplate(templateVariables, rawContent, false)
    invokeAndWaitIfNeeded { runWriteAction { VfsUtil.saveText(child, content) } }
  }

  /**
   * Checks if file exists in [baseDir] by given [path].
   * If it doesn't exist, creates a new file from internal [templateName] template.
   * Otherwise, substitutes all template variables in file text
   */
  @Throws(IOException::class)
  fun createFileFromTemplate(
    holder: CourseInfoHolder<out Course?>,
    baseDir: VirtualFile,
    path: String,
    templateName: String,
    templateVariables: Map<String, Any>
  ) {
    val file = baseDir.findFileByRelativePath(path)
    if (file == null) {
      val configText = getInternalTemplateText(templateName, templateVariables)
      createChildFile(holder, baseDir, path, TextualContents(configText))
    }
    else {
      evaluateExistingTemplate(file, templateVariables)
    }
  }

  /**
   * Removes [module] from [project].
   * It should be used when external build system like Gradle, sbt, etc. creates modules itself
   * and initial base module is unexpected while import
   */
  fun removeModule(project: Project, module: Module) {
    if (!isUnitTestMode || (project as? ProjectEx)?.isLight == false) {
      project.modifyModules { disposeModule(module) }
    }
  }

  /**
   * Reformat the code so that learners do not see tons of IDE highlighting.
   * Should be used for third-party sources of courses when language style guide is systematically ignored.
   * */
  @JvmStatic
  fun reformatCodeInAllTaskFiles(project: Project, course: Course) {
    course.visitTasks {
      for ((_, file) in it.taskFiles) {
        val virtualFile = file.getVirtualFile(project) ?: continue
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
        runInEdt {
          WriteCommandAction.runWriteCommandAction(project) {
            CodeStyleManager.getInstance(project).reformat(psiFile)
          }
        }
      }
    }
  }

  private val GRADLE_INVALID_SYMBOLS = "[ /\\\\:<>\"?*|()]".toRegex()
  private val LEADING_AND_TRAILING_DOTS = "(^[.]+)|([.]+\$)".toRegex()

  // Should be the same as `sanitizeName` in `resources/fileTemplates/internal/settings.gradle.ft`
  /**
   * Replaces ' ', '/', '\', ':', '<', '>', '"', '?', '*', '|', '(', ')' symbols with '_' as they are invalid in gradle module names
   * Also removes leading and trailing dots, because gradle project name must not start or end with a '.'
   */
  fun gradleSanitizeName(name: String): String = name.replace(GRADLE_INVALID_SYMBOLS, "_").replace(LEADING_AND_TRAILING_DOTS, "")

  fun getDefaultName(item: StudyItem) = when (item) {
    is Section -> "$SECTION${item.index}"
    is FrameworkLesson -> "${EduNames.FRAMEWORK_LESSON}${item.index}"
    is Lesson -> "$LESSON${item.index}"
    is Task -> "$TASK${item.index}"
    else -> "NonCommonStudyItem${item.index}"
  }
}
