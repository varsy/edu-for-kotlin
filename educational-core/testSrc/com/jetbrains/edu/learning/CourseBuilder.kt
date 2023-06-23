package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.configuration.PlainTextConfigurator
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.LESSON
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.SECTION
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TASK
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ONLY_IDEA_DIRECTORY
import com.jetbrains.edu.learning.stepik.course.StepikLesson
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import org.intellij.lang.annotations.Language
import java.io.File
import java.util.*
import java.util.regex.Pattern

private val OPENING_TAG: Pattern = Pattern.compile("<p>")
private val CLOSING_TAG: Pattern = Pattern.compile("</p>")

fun course(
  name: String = "Test Course",
  language: com.intellij.lang.Language = PlainTextLanguage.INSTANCE,
  description: String = "Test Course Description",
  environment: String = "",
  courseMode: CourseMode = CourseMode.STUDENT,
  courseProducer: () -> Course = ::EduCourse,
  buildCourse: CourseBuilder.() -> Unit
): Course {
  val builder = CourseBuilder(courseProducer())
  builder.withName(name)
  builder.withMode(courseMode)
  val course = builder.course
  course.languageId = language.id
  course.environment = environment
  course.description = description
  builder.buildCourse()
  return course
}

fun Course.createCourseFiles(
  project: Project,
  baseDir: VirtualFile = LightPlatformTestCase.getSourceRoot()
) {
  @Suppress("HardCodedStringLiteral")
  ProgressManager.getInstance().runProcessWithProgressSynchronously(Runnable {
    val holder = CourseInfoHolder.fromCourse(this, baseDir)

    configurator
      ?.courseBuilder
      ?.getCourseProjectGenerator(this)
      ?.createCourseStructure(holder)

    GeneratorUtils.unpackAdditionalFiles(holder, ONLY_IDEA_DIRECTORY)
  }, "Course Structure Generation", false, project)
}

abstract class LessonOwnerBuilder(val course: Course) {

  protected abstract val nextLessonIndex: Int
  protected abstract fun addLesson(lesson: Lesson)

  fun frameworkLesson(
    name: String? = null,
    customPresentableName: String? = null,
    isTemplateBased: Boolean = true,
    id: Int = 0,
    buildLesson: LessonBuilder<FrameworkLesson>.() -> Unit = {}
  ) {
    val lesson = FrameworkLesson().also { it.isTemplateBased = isTemplateBased }
    lesson(lesson, name, customPresentableName, id, buildLesson)
  }

  fun stepikLesson(
    name: String? = null,
    customPresentableName: String? = null,
    buildLesson: LessonBuilder<Lesson>.() -> Unit = {}
  ) {
    val stepikLesson = StepikLesson()
    lesson(stepikLesson, name, customPresentableName, buildLesson = buildLesson)
  }

  fun lesson(
    name: String? = null,
    customPresentableName: String? = null,
    id: Int = 0,
    buildLesson: LessonBuilder<Lesson>.() -> Unit = {}
  ) {
    lesson(Lesson(), name, customPresentableName, id, buildLesson)
  }

  fun station(
    name: String? = null,
    customPresentableName: String? = null,
    buildLesson: LessonBuilder<CheckiOStation>.() -> Unit = {}
  ) {
    lesson(CheckiOStation(), name, customPresentableName, buildLesson = buildLesson)
  }

  protected fun <T : Lesson> lesson(
    lesson: T,
    name: String? = null,
    customPresentableName: String? = null,
    id: Int = 0,
    buildLesson: LessonBuilder<T>.() -> Unit
  ) {
    val lessonBuilder = LessonBuilder(course, null, lesson)
    lesson.index = nextLessonIndex
    lessonBuilder.withName(name ?: (LESSON + nextLessonIndex))
    lessonBuilder.withCustomPresentableName(customPresentableName)
    lessonBuilder.withId(id)
    addLesson(lesson)
    lessonBuilder.buildLesson()
  }

  fun additionalFiles(buildEduFiles: AdditionalFilesBuilder.() -> Unit) {
    val builder = AdditionalFilesBuilder(course)
    builder.buildEduFiles()
  }
}

class CourseBuilder(course: Course) : LessonOwnerBuilder(course) {

  override val nextLessonIndex: Int get() = course.lessons.size + 1

  fun withName(name: String) {
    course.name = name
  }

  fun withMode(courseMode: CourseMode) {
    course.courseMode = courseMode
  }

  override fun addLesson(lesson: Lesson) {
    course.addLesson(lesson)
  }

  fun section(
    name: String? = null,
    customPresentableName: String? = null,
    buildSection: SectionBuilder.() -> Unit = {}
  ) {
    val sectionBuilder = SectionBuilder(course, Section())
    val section = sectionBuilder.section
    section.index = course.items.size + 1
    val nextSectionIndex = course.items.size + 1
    sectionBuilder.withName(name ?: (SECTION + nextSectionIndex))
    sectionBuilder.withCustomPresentableName(customPresentableName)
    course.addSection(section)
    sectionBuilder.buildSection()
  }

  fun additionalFile(name: String, text: String, buildTaskFile: EduFileBuilder.() -> Unit = {}) =
    additionalFile(name, UndeterminedContents(text), buildTaskFile)

  fun additionalFile(name: String, contents: FileContents = UndeterminedContents.EMPTY, buildTaskFile: EduFileBuilder.() -> Unit = {}) {
    val builder = EduFileBuilder()
    builder.withName(name)
    builder.withContents(contents)
    builder.buildTaskFile()

    course.additionalFiles = course.additionalFiles + builder.eduFile
  }

  fun environmentSetting(key: String, value: String) {
    course.environmentSettings = course.environmentSettings.plus(key to value)
  }
}

class SectionBuilder(course: Course, val section: Section = Section()) : LessonOwnerBuilder(course) {
  init {
    section.parent = course
  }

  override val nextLessonIndex: Int get() = section.lessons.size + 1

  override fun addLesson(lesson: Lesson) {
    section.addLesson(lesson)
  }

  fun withName(name: String) {
    section.name = name
  }

  fun withCustomPresentableName(name: String?) {
    section.customPresentableName = name
  }
}

class LessonBuilder<T : Lesson>(val course: Course, section: Section?, val lesson: T) {

  init {
    lesson.parent = section ?: course
  }

  fun withName(name: String) {
    lesson.name = name
  }

  fun withCustomPresentableName(name: String?) {
    lesson.customPresentableName = name
  }

  fun withId(id: Int) {
    lesson.id = id
  }

  private fun task(
    task: Task,
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    // we want to know task files order in tests
    task.taskFiles = LinkedHashMap()
    val taskBuilder = TaskBuilder(lesson, task)
    taskBuilder.task.index = lesson.taskList.size + 1
    val nextTaskIndex = lesson.taskList.size + 1
    taskBuilder.withName(name ?: (TASK + nextTaskIndex))
    taskBuilder.withCustomPresentableName(customPresentableName)
    val descriptionFormat = if (task.course is HyperskillCourse) DescriptionFormat.HTML else taskDescriptionFormat
    taskBuilder.withTaskDescription(taskDescription ?: "solve task", descriptionFormat)
    taskBuilder.withStepId(stepId)
    taskBuilder.withUpdateDate(updateDate)
    taskBuilder.buildTask()
    for ((_, taskFile) in taskBuilder.task.taskFiles) {
      taskFile.isVisible = taskBuilder.explicitVisibility[taskFile.name] ?: !EduUtilsKt.isTestsFile(taskBuilder.task, taskFile.name)
    }

    lesson.addTask(taskBuilder.task)
  }

  fun eduTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(EduTask(), name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun remoteEduTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    checkProfile: String = "",
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val remoteEduTask = RemoteEduTask()
    task(remoteEduTask, name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
    remoteEduTask.checkProfile = checkProfile
  }

  fun theoryTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(TheoryTask(), name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun outputTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(OutputTask(), name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun choiceTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    choiceOptions: Map<String, ChoiceOptionStatus>,
    isMultipleChoice: Boolean = false,
    selectedVariants: MutableList<Int> = mutableListOf(),
    status: CheckStatus = CheckStatus.Unchecked,
    messageCorrect: String? = null,
    messageIncorrect: String? = null,
    quizHeader: String? = null,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val choiceTask = ChoiceTask()
    task(choiceTask, name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
    if (stepId != 0) choiceTask.canCheckLocally = false
    choiceTask.choiceOptions = choiceOptions.map { ChoiceOption(it.key, it.value) }
    choiceTask.isMultipleChoice = isMultipleChoice
    choiceTask.selectedVariants = selectedVariants
    choiceTask.status = status
    quizHeader?.also {choiceTask.quizHeader = it }
    messageCorrect?.also {choiceTask.messageCorrect = it }
    messageIncorrect?.also {choiceTask.messageIncorrect = it }
  }

  fun stringTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val stringTask = StringTask()
    task(stringTask, name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
  }

  fun numberTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val numberTask = NumberTask()
    task(numberTask, name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
  }

  fun mission(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    code: String = "",
    secondsFromChange: Long = 0,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val mission = CheckiOMission()
    task(mission, name, customPresentableName, taskDescription, taskDescriptionFormat, buildTask = buildTask)
    mission.code = code
    mission.secondsFromLastChangeOnServer = secondsFromChange
  }

  fun codeTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(CodeTask(), name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun codeforcesTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val codeforcesTask = CodeforcesTask()
    task(codeforcesTask, name, customPresentableName, taskDescription, DescriptionFormat.HTML, buildTask = buildTask)
  }

  fun dataTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    attempt: DataTaskAttempt? = null,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val dataTask = DataTask()
    task(dataTask, name, customPresentableName, taskDescription, DescriptionFormat.HTML, stepId, updateDate, buildTask)
    dataTask.attempt = attempt
  }

  fun sortingTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    options: List<String> = emptyList(),
    ordering: IntArray = intArrayOf(),
    status: CheckStatus = CheckStatus.Unchecked,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val sortingTask = SortingTask()
    task(sortingTask, name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
    sortingTask.options = options
    sortingTask.ordering = ordering
    sortingTask.status = status
  }

  fun matchingTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    captions: List<String> = emptyList(),
    options: List<String> = emptyList(),
    ordering: IntArray = intArrayOf(),
    status: CheckStatus = CheckStatus.Unchecked,
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val task = MatchingTask()
    task(task, name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
    task.options = options
    task.ordering = ordering
    task.status = status
    task.captions = captions
  }

  fun remoteEduTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val remoteEduTask = RemoteEduTask()
    task(remoteEduTask, name, customPresentableName, taskDescription, DescriptionFormat.HTML, stepId, updateDate, buildTask)
  }
}

class TaskBuilder(val lesson: Lesson, val task: Task) {

  private val _explicitVisibility: MutableMap<String, Boolean> = mutableMapOf()

  val explicitVisibility: Map<String, Boolean> get() = _explicitVisibility

  init {
    task.parent = lesson
  }

  fun withName(name: String) {
    task.name = name
  }

  fun withCustomPresentableName(name: String?) {
    task.customPresentableName = name
  }

  fun withTaskDescription(text: String, format: DescriptionFormat? = null) {
    task.descriptionText = text
    task.descriptionFormat = format ?: DescriptionFormat.MD
  }

  fun withUpdateDate(date: Date) {
    task.updateDate = date
  }

  fun withStepId(stepId: Int) {
    task.id = stepId
  }

  /**
   * Creates task file with given [name] and [text].
   *
   * You can also create placeholders for this task file using `<p>` tag.
   *
   * For example, for `fun foo() = <p>TODO()</p>` text
   * it creates task file with `fun foo() = TODO()` text and placeholder with `TODO()` as placeholder text.
   */
  fun taskFile(
    name: String, text: String,
    visible: Boolean? = null,
    editable: Boolean? = true,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, UndeterminedContents(text), visible, editable, buildTaskFile)

  fun taskFile(
    name: String, contents: FileContents = UndeterminedContents.EMPTY,
    visible: Boolean? = null,
    editable: Boolean? = true,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) {
    val taskFileBuilder = TaskFileBuilder(task)
    taskFileBuilder.withName(name)

    if (contents.isBinary != true) {
      val textBuilder = StringBuilder(contents.textualRepresentation.trimIndent())
      val placeholders = extractPlaceholdersFromText(textBuilder)
      taskFileBuilder.withContents(TextualContents(textBuilder.toString()))
      taskFileBuilder.withPlaceholders(placeholders)
    }
    else {
      taskFileBuilder.withContents(contents)
    }

    taskFileBuilder.buildTaskFile()
    val taskFile = taskFileBuilder.taskFile
    if (visible != null) {
      _explicitVisibility[name] = visible
    }
    taskFile.task = task
    taskFile.isEditable = editable ?: true
    task.addTaskFile(taskFile)
  }

  fun checkResultFile(status: CheckStatus, message: String = "") {
    taskFile(PlainTextConfigurator.CHECK_RESULT_FILE, "$status $message")
  }

  fun kotlinTaskFile(
    name: String,
    @Language("kotlin") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun javaTaskFile(
    name: String,
    @Language("JAVA") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun pythonTaskFile(
    name: String,
    @Language("Python") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun scalaTaskFile(
    name: String,
    @Language("Scala") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun rustTaskFile(
    name: String,
    @Language("Rust") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun goTaskFile(
    name: String,
    @Language("Go") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun cppTaskFile(
    name: String,
    @Language("ObjectiveC") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun xmlTaskFile(
    name: String,
    @Language("XML") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun sqlTaskFile(
    name: String,
    @Language("SQL") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, text, visible, buildTaskFile = buildTaskFile)

  fun taskFileFromResources(
    disposable: Disposable,
    name: String,
    path: String,
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) {
    val ioFile = File(path)
    VfsRootAccess.allowRootAccess(disposable, ioFile.absolutePath)
    val file = LocalFileSystem.getInstance().findFileByIoFile(ioFile) ?: error("Can't find `$path`")
    val text = file.loadEncodedContent()
    taskFile(name, text, visible, buildTaskFile = buildTaskFile)
  }

  fun dir(dirName: String, buildTask: TaskBuilder.() -> Unit) {
    val tmpTask = EduTask()
    val innerBuilder = TaskBuilder(lesson, tmpTask)
    innerBuilder.buildTask()
    for ((_, taskFile) in tmpTask.taskFiles) {
      val visibility = innerBuilder.explicitVisibility[taskFile.name]
      taskFile.name = "$dirName/${taskFile.name}"
      task.addTaskFile(taskFile)
      if (visibility != null) {
        _explicitVisibility[taskFile.name] = visibility
      }
    }
  }

  private fun extractPlaceholdersFromText(text: StringBuilder): List<AnswerPlaceholder> {
    val openingMatcher = OPENING_TAG.matcher(text)
    val closingMatcher = CLOSING_TAG.matcher(text)
    val placeholders = mutableListOf<AnswerPlaceholder>()
    var pos = 0
    while (openingMatcher.find(pos)) {
      val answerPlaceholder = AnswerPlaceholder()
      if (!closingMatcher.find(openingMatcher.end())) {
        error("No matching closing tag found")
      }
      answerPlaceholder.offset = openingMatcher.start()
      answerPlaceholder.length = closingMatcher.start() - openingMatcher.end()
      answerPlaceholder.placeholderText = text.substring(openingMatcher.end(), closingMatcher.start())
      placeholders.add(answerPlaceholder)
      text.delete(closingMatcher.start(), closingMatcher.end())
      text.delete(openingMatcher.start(), openingMatcher.end())
      pos = answerPlaceholder.endOffset
    }
    return placeholders
  }
}

class AdditionalFilesBuilder(val course: Course) {

  fun eduFile(
    name: String, text: String = "",
    buildEduFile: EduFileBuilder.() -> Unit = {}
  ) {
    val eduFileBuilder = EduFileBuilder()
    eduFileBuilder.withName(name)
    val textBuilder = StringBuilder(text.trimIndent())
    eduFileBuilder.withText(textBuilder.toString())
    eduFileBuilder.buildEduFile()
    val taskFile = eduFileBuilder.eduFile
    course.additionalFiles = course.additionalFiles + taskFile
  }

}

class TaskFileBuilder(val task: Task? = null) {
  val taskFile = TaskFile()

  init {
    if (task != null) {
      taskFile.task = task
    }
  }

  fun withName(name: String) {
    taskFile.name = name
  }

  fun withText(text: String) {
    taskFile.text = text
  }

  fun withContents(contents: FileContents) {
    taskFile.contents = contents
  }

  fun withPlaceholders(placeholders: List<AnswerPlaceholder>) {
    for (placeholder in placeholders) {
      placeholder.taskFile = taskFile
      taskFile.addAnswerPlaceholder(placeholder)
    }
  }

  fun withHighlightLevel(highlightLevel: EduFileErrorHighlightLevel) {
    taskFile.errorHighlightLevel = highlightLevel
  }

  fun placeholder(index: Int, possibleAnswer: String? = null, placeholderText: String? = null,
                  dependency: String = "", isVisible: Boolean = true) {
    val answerPlaceholder = taskFile.answerPlaceholders[index]
    if (possibleAnswer != null) {
      answerPlaceholder.possibleAnswer = possibleAnswer
    }
    if (placeholderText != null) {
      answerPlaceholder.placeholderText = placeholderText
    }
    val createdDependency = AnswerPlaceholderDependency.create(answerPlaceholder, dependency, isVisible)
    if (createdDependency != null) {
      answerPlaceholder.placeholderDependency = createdDependency
    }
  }
}

class EduFileBuilder {
  val eduFile = EduFile()

  fun withName(name: String) {
    eduFile.name = name
  }

  @Deprecated("Use withContents to specify type", replaceWith = ReplaceWith("withContents()"))
  fun withText(text: String) {
    eduFile.text = text
  }

  fun withContents(contents: FileContents) {
    eduFile.contents = contents
  }
}
