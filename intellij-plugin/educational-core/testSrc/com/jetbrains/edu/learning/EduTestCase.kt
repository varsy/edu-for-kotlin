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
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EducationalExtensionPoint
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.configurators.FakeGradleHyperskillConfigurator
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSERA
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.STEPIK
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import okhttp3.mockwebserver.MockResponse
import org.apache.http.HttpStatus
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import kotlin.reflect.KMutableProperty0

@RunWith(JUnit4::class)
abstract class EduTestCase : BasePlatformTestCase() {

  protected lateinit var testPluginDescriptor: IdeaPluginDescriptor

  protected open val useDocumentListener: Boolean = true

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    testPluginDescriptor = PluginManager.getPlugins().first { it.pluginId.idString.startsWith("com.jetbrains.edu") }
    // In this method course is set before course files are created so `CCProjectComponent.createYamlConfigFilesIfMissing` is called
    // for course with no files. This flag is checked in this method and it does nothing if the flag is false
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE, HYPERSKILL)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE, STEPIK)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE,
                         environment = EduNames.ANDROID)
    registerConfigurator(myFixture.testRootDisposable, PlainTextConfigurator::class.java, PlainTextLanguage.INSTANCE, COURSERA)
    registerConfigurator(myFixture.testRootDisposable, FakeGradleConfigurator::class.java, FakeGradleBasedLanguage)
    registerConfigurator(myFixture.testRootDisposable, FakeGradleHyperskillConfigurator::class.java, FakeGradleBasedLanguage, HYPERSKILL)

    // Mock tool window provided by default headless implementation of `ToolWindowManager` doesn't keep any state.
    // As a result, it's impossible to write tests which check tool window state.
    // `EduToolWindowHeadlessManager` allows us to track necessary properties in our tests
    project.replaceService(ToolWindowManager::class.java, EduToolWindowHeadlessManager(project), testRootDisposable)

    EduTestServiceStateHelper.restoreState(project)

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

    createCourse()
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, true)
  }

  override fun tearDown() {
    try {
      EduTestServiceStateHelper.cleanUpState(project)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
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
    TaskToolWindowView.getInstance(myFixture.project).currentTask = myFixture.project.getCurrentTask()
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

  protected fun getLessons(lessonContainer: LessonContainer? = null): List<Lesson> {
    val container = lessonContainer ?: getCourse()
    return container.lessons
  }

  protected fun findFileInTask(lessonIndex: Int, taskIndex: Int, taskFilePath: String): VirtualFile {
    return findTask(lessonIndex, taskIndex).getTaskFile(taskFilePath)?.getVirtualFile(project)!!
  }

  protected fun findFile(path: String): VirtualFile =
    LightPlatformTestCase.getSourceRoot().findFileByRelativePath(path) ?: error("Can't find `$path`")

  protected fun Task.openTaskFileInEditor(taskFilePath: String, placeholderIndex: Int? = null) {
    val taskFile = getTaskFile(taskFilePath) ?: error("Can't find task file `$taskFilePath` in `$name`")
    val file = taskFile.getVirtualFile(project) ?: error("Can't find virtual file for `${taskFile.name}` task")
    myFixture.openFileInEditor(file)
    TaskToolWindowView.getInstance(myFixture.project).currentTask = this
    if (placeholderIndex != null) {
      val placeholder = taskFile.answerPlaceholders[placeholderIndex]
      myFixture.editor.selectionModel.setSelection(placeholder.offset, placeholder.endOffset)
    }
  }

  protected fun Task.createTaskFileAndOpenInEditor(taskFilePath: String, text: String = "") {
    val taskDir = getDir(project.courseDir) ?: error("Can't find task dir")
    val file = GeneratorUtils.createTextChildFile(project, taskDir, taskFilePath, text) ?: error("Failed to create `$taskFilePath` in $taskDir")
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
    var idCounter = 0

    val remoteCourse = EduCourse()
    remoteCourse.id = ++idCounter
    remoteCourse.name = name
    remoteCourse.courseMode = courseMode
    remoteCourse.items = Lists.newArrayList(items)
    remoteCourse.languageId = languageId
    remoteCourse.languageVersion = languageVersion
    remoteCourse.description = description
    remoteCourse.additionalFiles = additionalFiles

    for (item in remoteCourse.items) {
      if (item is Section) {
        item.id = ++idCounter
        for (lesson in item.lessons) {
          lesson.id = ++idCounter
          for (task in lesson.taskList) {
            task.id = ++idCounter
          }
        }
      }

      if (item is Lesson) {
        item.id = ++idCounter
        for (task in item.taskList) {
          task.id = ++idCounter
        }
      }
    }

    remoteCourse.sectionIds = remoteCourse.sections.map { it.id }

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

  /**
   * Manually register a tool window in [ToolWindowHeadlessManagerImpl] by its id.
   *
   * In tests, tool windows are not registered by default.
   * So if you need any tool window in `ToolWindowManager` in tests,
   * you have to register it manually.
   */
  protected fun registerToolWindow(id: String) {
    val toolWindowManager = ToolWindowManager.getInstance(project) as ToolWindowHeadlessManagerImpl
    if (toolWindowManager.getToolWindow(id) == null) {
      for (bean in collectToolWindowExtensions()) {
        if (bean.id == id) {
          toolWindowManager.doRegisterToolWindow(bean.id)
          Disposer.register(testRootDisposable) {
            toolWindowManager.unregisterToolWindow(bean.id)
          }
        }
      }
    }
  }

  protected fun registerTaskDescriptionToolWindow() {
    registerToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW)
  }
}
