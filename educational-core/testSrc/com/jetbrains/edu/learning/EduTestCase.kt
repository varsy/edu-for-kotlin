package com.jetbrains.edu.learning

import com.google.common.collect.Lists
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.update.CodeforcesCourseUpdateChecker
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EducationalExtensionPoint
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.configurators.FakeGradleHyperskillConfigurator
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.framework.impl.FrameworkLessonManagerImpl
import com.jetbrains.edu.learning.marketplace.update.MarketplaceUpdateChecker
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlLoadingErrorManager
import okhttp3.mockwebserver.MockResponse
import org.apache.http.HttpStatus
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import javax.swing.Icon
import kotlin.reflect.KMutableProperty0

abstract class EduTestCase : BasePlatformTestCase() {

  protected lateinit var testPluginDescriptor: IdeaPluginDescriptor

  protected open val useDocumentListener: Boolean = true

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    testPluginDescriptor = PluginManager.getPlugins().first { it.pluginId.idString.startsWith("com.jetbrains.edu") }
    StudyTaskManager.getInstance(project).course = null // if a test doesn't set any course, course from the previous test stays incorrectly
    SubmissionsManager.getInstance(project).clear()
    // In this method course is set before course files are created so `CCProjectComponent.createYamlConfigFilesIfMissing` is called
    // for course with no files. This flag is checked in this method and it does nothing if the flag is false
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE, HYPERSKILL)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         CheckiONames.CHECKIO_TYPE)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         StepikNames.STEPIK_TYPE)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         environment = EduNames.ANDROID)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         CourseraNames.COURSE_TYPE)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         CodeforcesNames.CODEFORCES_COURSE_TYPE)
    registerConfigurator(myFixture.testRootDisposable, FakeGradleConfigurator::class.java, FakeGradleBasedLanguage)
    registerConfigurator(myFixture.testRootDisposable, FakeGradleHyperskillConfigurator::class.java, FakeGradleBasedLanguage, HYPERSKILL)

    // File types are used to detect, whether files are binary or textual.
    // In tests, much less plugins are installed, and thus much less file types are known
    setupFileTypes()

    CheckActionListener.reset()
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
      override fun courseSet(course: Course) {
        if (useDocumentListener) {
          EduDocumentListener.setGlobalListener(project, testRootDisposable)
        }
        connection.disconnect()
      }
    })
    val frameworkLessonManagerImpl = FrameworkLessonManager.getInstance(project) as FrameworkLessonManagerImpl
    frameworkLessonManagerImpl.storage = FrameworkLessonManagerImpl.createStorage(project)
    createCourse()
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, true)
  }

  private fun setupFileTypes() {
    runWriteAction {
      associateFileType("SVG", false)
      associateFileType("PNG", true)
    }
  }

  private fun associateFileType(extension: String, isBinary: Boolean) {
    FileTypeManager.getInstance().associate(object : FileType {
      override fun getName(): String = extension
      override fun getDescription(): String = extension
      override fun getDefaultExtension(): String = extension
      override fun getIcon(): Icon = TODO("Not expected to be called")
      override fun isBinary(): Boolean = isBinary
    }, ExtensionFileNameMatcher(extension))
  }

  override fun tearDown() {
    try {
      (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl = null
      SubmissionsManager.getInstance(project).clear()

      TaskDescriptionView.getInstance(project).currentTask = null
      val storage = (FrameworkLessonManager.getInstance(project) as FrameworkLessonManagerImpl).storage
      Disposer.dispose(storage)
      YamlLoadingErrorManager.getInstance(project).removeAllErrors()

      MarketplaceUpdateChecker.getInstance(project).course = null
      HyperskillCourseUpdateChecker.getInstance(project).course = null
      CodeforcesCourseUpdateChecker.getInstance(project).course = null
    }
    finally {
      super.tearDown()
    }
  }

  @Throws(IOException::class)
  protected open fun createCourse() {
    val course = EduCourse()
    course.name = "Edu test course"
    course.languageId = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(project).course = course
  }

  @Throws(IOException::class)
  protected fun createLesson(index: Int, taskCount: Int): Lesson {
    val lesson = Lesson()
    lesson.name = "lesson$index"
    (1..taskCount)
      .map { createTask(index, it) }
      .forEach { lesson.addTask(it) }
    lesson.index = index
    return lesson
  }

  @Throws(IOException::class)
  private fun createTask(lessonIndex: Int, taskIndex: Int): Task {
    val task = EduTask()
    task.name = "task$taskIndex"
    task.index = taskIndex
    createTaskFile(lessonIndex, task, "taskFile$taskIndex.txt")
    return task
  }

  @Throws(IOException::class)
  private fun createTaskFile(lessonIndex: Int, task: Task, taskFilePath: String) {
    val taskFile = TaskFile()
    taskFile.task = task
    taskFile.name = taskFilePath
    task.addTaskFile(taskFile)

    val fileName = "lesson" + lessonIndex + "/" + task.name + "/" + taskFilePath
    val file = myFixture.findFileInTempDir(fileName)
    taskFile.text = VfsUtilCore.loadText(file)

    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
    try {
      val document = FileDocumentManager.getInstance().getDocument(file)!!
      for (placeholder in getPlaceholders(document, true)) {
        taskFile.addAnswerPlaceholder(placeholder)
      }
    }
    finally {
      FileEditorManager.getInstance(myFixture.project).closeFile(file)
    }
    taskFile.sortAnswerPlaceholders()
  }

  /**
   * Be aware: this method overrides any selection in editor
   * because [com.intellij.testFramework.fixtures.CodeInsightTestFixture.configureFromExistingVirtualFile] loads selection and caret from markup in text
   */
  protected fun configureByTaskFile(lessonIndex: Int, taskIndex: Int, taskFileName: String) {
    val fileName = "lesson$lessonIndex/task$taskIndex/$taskFileName"
    val file = myFixture.findFileInTempDir(fileName)
    myFixture.configureFromExistingVirtualFile(file)
    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
    TaskDescriptionView.getInstance(myFixture.project).currentTask = myFixture.project.getCurrentTask()
  }

  override fun getTestDataPath(): String {
    return "testData"
  }

  fun courseWithFiles(
    name: String = "Test Course",
    courseMode: CourseMode = CourseMode.STUDENT,
    id: Int? = null,
    description: String = "Test Course Description",
    environment: String = "",
    language: Language = PlainTextLanguage.INSTANCE,
    courseVendor: Vendor? = null,
    courseProducer: () -> Course = ::EduCourse,
    createYamlConfigs: Boolean = false,
    buildCourse: CourseBuilder.() -> Unit
  ): Course {
    val course = course(name, language, description, environment, courseMode, courseProducer, buildCourse).apply {
      vendor = courseVendor

      initializeCourse(project, course)
      createCourseFiles(project)
      if (createYamlConfigs) {
        createConfigFiles(project)
      }
    }
    if (id != null) {
      course.id = id
    }

    SubmissionsManager.getInstance(project).course = course
    return course
  }

  protected fun getCourse(): Course = StudyTaskManager.getInstance(project).course!!

  protected fun findPlaceholder(lessonIndex: Int, taskIndex: Int, taskFile: String, placeholderIndex: Int): AnswerPlaceholder =
    findTaskFile(lessonIndex, taskIndex, taskFile).answerPlaceholders[placeholderIndex]

  protected fun findTaskFile(lessonIndex: Int, taskIndex: Int, taskFile: String): TaskFile =
    getCourse().lessons[lessonIndex].taskList[taskIndex].taskFiles[taskFile]!!

  protected fun findTask(lessonIndex: Int, taskIndex: Int): Task = findLesson(lessonIndex).taskList[taskIndex]

  protected fun findTask(sectionIndex: Int,
                         lessonIndex: Int,
                         taskIndex: Int): Task = getCourse().sections[sectionIndex].lessons[lessonIndex].taskList[taskIndex]

  protected fun findLesson(lessonIndex: Int): Lesson = getCourse().lessons[lessonIndex]

  protected fun findFileInTask(lessonIndex: Int, taskIndex: Int, taskFilePath: String): VirtualFile {
    return findTask(lessonIndex, taskIndex).getTaskFile(taskFilePath)?.getVirtualFile(project)!!
  }

  protected fun findFile(path: String): VirtualFile =
    LightPlatformTestCase.getSourceRoot().findFileByRelativePath(path) ?: error("Can't find `$path`")

  protected fun Task.openTaskFileInEditor(taskFilePath: String, placeholderIndex: Int? = null) {
    val taskFile = getTaskFile(taskFilePath) ?: error("Can't find task file `$taskFilePath` in `$name`")
    val file = taskFile.getVirtualFile(project) ?: error("Can't find virtual file for `${taskFile.name}` task")
    myFixture.openFileInEditor(file)
    TaskDescriptionView.getInstance(myFixture.project).currentTask = this
    if (placeholderIndex != null) {
      val placeholder = taskFile.answerPlaceholders[placeholderIndex]
      myFixture.editor.selectionModel.setSelection(placeholder.offset, placeholder.endOffset)
    }
  }

  protected fun Task.createTaskFileAndOpenInEditor(taskFilePath: String, text: String = "") {
    val taskDir = getDir(project.courseDir) ?: error("Can't find task dir")
    val file = GeneratorUtils.createChildFile(project, taskDir, taskFilePath, text) ?: error("Failed to create `$taskFilePath` in $taskDir")
    myFixture.openFileInEditor(file)
  }

  protected fun Task.removeTaskFile(taskFilePath: String) {
    require(getTaskFile(taskFilePath) != null) {
      "Can't find `$taskFilePath` task file in $name task"
    }
    val taskDir = getDir(project.courseDir) ?: error("Can't find task dir")
    val file = taskDir.findFileByRelativePath(taskFilePath) ?: error("Can't find `$taskFilePath` in `$taskDir`")
    runWriteAction { file.delete(this) }
  }

  protected inline fun withVirtualFileListener(course: Course, action: () -> Unit) {
    withVirtualFileListener(project, course, testRootDisposable, action)
  }

  protected fun Course.asRemote(courseMode: CourseMode = CourseMode.EDUCATOR): EduCourse {
    val remoteCourse = EduCourse()
    remoteCourse.id = 1
    remoteCourse.name = name
    remoteCourse.courseMode = courseMode
    remoteCourse.items = Lists.newArrayList(items)
    remoteCourse.languageId = languageId
    remoteCourse.languageVersion = languageVersion
    remoteCourse.description = description

    var hasSections = false
    for (item in remoteCourse.items) {
      if (item is Section) {
        item.id = item.index
        for (lesson in item.lessons) {
          lesson.id = lesson.index
          for (task in lesson.taskList) {
            task.id = task.index
          }
        }
        hasSections = true
      }

      if (item is Lesson) {
        item.id = item.index
        for (task in item.taskList) {
          task.id = task.index
        }
      }
    }

    if (!hasSections) {
      remoteCourse.sectionIds = listOf(1)
    }
    remoteCourse.init(true)
    StudyTaskManager.getInstance(project).course = remoteCourse
    return remoteCourse
  }

  private fun registerConfigurator(
    disposable: Disposable,
    configuratorClass: Class<*>,
    language: Language,
    courseType: String = EduFormatNames.PYCHARM,
    environment: String = DEFAULT_ENVIRONMENT
  ) {
    val extension = EducationalExtensionPoint<EduConfigurator<*>>()
    extension.language = language.id
    extension.implementationClass = configuratorClass.name
    extension.courseType = courseType
    extension.environment = environment
    extension.pluginDescriptor = testPluginDescriptor
    EducationalExtensionPoint.EP_NAME.point.registerExtension(extension, disposable)
  }

  protected fun getTestFile(fileName: String) = testDataPath + fileName

  protected fun mockResponse(fileName: String, responseCode: Int = HttpStatus.SC_OK): MockResponse =
    MockResponseFactory.fromFile(getTestFile(fileName), responseCode)

  @Throws(IOException::class)
  protected fun loadText(fileName: String): String = FileUtil.loadFile(File(testDataPath, fileName))

  protected fun checkFileTree(block: FileTreeBuilder.() -> Unit) {
    fileTree(block).assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  companion object {

    fun getPlaceholders(document: Document, useLength: Boolean): List<AnswerPlaceholder> {
      return WriteCommandAction.writeCommandAction(null).compute<List<AnswerPlaceholder>, RuntimeException> {
        val placeholders = mutableListOf<AnswerPlaceholder>()
        val openingTagRx = "<placeholder( taskText=\"(.+?)\")?( possibleAnswer=\"(.+?)\")?( hint=\"(.+?)\")?( hint2=\"(.+?)\")?>"
        val closingTagRx = "</placeholder>"
        val text = document.charsSequence
        val openingMatcher = Pattern.compile(openingTagRx).matcher(text)
        val closingMatcher = Pattern.compile(closingTagRx).matcher(text)
        var pos = 0
        while (openingMatcher.find(pos)) {
          val answerPlaceholder = AnswerPlaceholder()
          val taskText = openingMatcher.group(2)
          if (taskText != null) {
            answerPlaceholder.placeholderText = taskText
            answerPlaceholder.length = taskText.length
          }
          var possibleAnswer = openingMatcher.group(4)
          if (possibleAnswer != null) {
            answerPlaceholder.possibleAnswer = possibleAnswer
          }
          answerPlaceholder.offset = openingMatcher.start()
          if (!closingMatcher.find(openingMatcher.end())) {
            LOG.error("No matching closing tag found")
          }
          var length: Int
          if (useLength) {
            answerPlaceholder.placeholderText = text.substring(openingMatcher.end(), closingMatcher.start())
            answerPlaceholder.length = closingMatcher.start() - openingMatcher.end()
            length = answerPlaceholder.length
          }
          else {
            if (possibleAnswer == null) {
              possibleAnswer = document.getText(TextRange.create(openingMatcher.end(), closingMatcher.start()))
              answerPlaceholder.possibleAnswer = possibleAnswer
              answerPlaceholder.length = possibleAnswer.length
            }
            length = answerPlaceholder.possibleAnswer.length
          }
          document.deleteString(closingMatcher.start(), closingMatcher.end())
          document.deleteString(openingMatcher.start(), openingMatcher.end())
          FileDocumentManager.getInstance().saveDocument(document)
          placeholders.add(answerPlaceholder)
          pos = answerPlaceholder.offset + length
        }
        placeholders
      }
    }
  }

  protected fun <T, V> withSettingsValue(property: KMutableProperty0<V>, value: V, action: () -> T): T {
    val oldValue = property.get()
    property.set(value)
    return try {
      action()
    }
    finally {
      property.set(oldValue)
    }
  }

  protected fun <T> withDefaultHtmlTaskDescription(action: () -> T): T {
    return withSettingsValue(CCSettings.getInstance()::useHtmlAsDefaultTaskFormat, true, action)
  }
}
