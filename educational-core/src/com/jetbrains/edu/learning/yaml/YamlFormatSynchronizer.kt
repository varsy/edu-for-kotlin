package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency
import com.jetbrains.edu.learning.courseFormat.CheckFeedback
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.getEditor
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.json.encrypt.TEST_AES_KEY
import com.jetbrains.edu.learning.json.encrypt.getAesKey
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.course.StepikLesson
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.TASK_CONFIG
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderDependencyYamlMixin
import com.jetbrains.edu.learning.yaml.format.AnswerPlaceholderYamlMixin
import com.jetbrains.edu.learning.yaml.format.ChoiceOptionYamlMixin
import com.jetbrains.edu.learning.yaml.format.ChoiceTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.CodeTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.CodeforcesCourseRemoteInfoYamlMixin
import com.jetbrains.edu.learning.yaml.format.CodeforcesCourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.CourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.CourseraCourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.DataTaskAttemptYamlMixin
import com.jetbrains.edu.learning.yaml.format.EduCourseRemoteInfoYamlMixin
import com.jetbrains.edu.learning.yaml.format.FrameworkLessonYamlMixin
import com.jetbrains.edu.learning.yaml.format.HyperskillCourseMixin
import com.jetbrains.edu.learning.yaml.format.HyperskillProjectMixin
import com.jetbrains.edu.learning.yaml.format.HyperskillStageMixin
import com.jetbrains.edu.learning.yaml.format.HyperskillTopicMixin
import com.jetbrains.edu.learning.yaml.format.LessonYamlMixin
import com.jetbrains.edu.learning.yaml.format.RemoteDataTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.RemoteEduTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.RemoteStudyItemYamlMixin
import com.jetbrains.edu.learning.yaml.format.SectionYamlMixin
import com.jetbrains.edu.learning.yaml.format.StepikLessonRemoteYamlMixin
import com.jetbrains.edu.learning.yaml.format.StepikLessonYamlMixin
import com.jetbrains.edu.learning.yaml.format.TaskFileYamlMixin
import com.jetbrains.edu.learning.yaml.format.TaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.TheoryTaskYamlUtil
import com.jetbrains.edu.learning.yaml.format.student.CheckiOMissionYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.CheckiOStationYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.CodeforcesTaskWithFileIOYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.CodeforcesTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.FeedbackYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.InitialStateMixin
import com.jetbrains.edu.learning.yaml.format.student.MatchingTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.SortingTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.StudentAnswerPlaceholderYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.StudentChoiceTaskYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.StudentCourseYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.StudentEncryptedAnswerPlaceholderYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.StudentFrameworkLessonYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskFileYamlMixin
import com.jetbrains.edu.learning.yaml.format.student.StudentTaskYamlMixin
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel

object YamlFormatSynchronizer {
  val LOAD_FROM_CONFIG = Key<Boolean>("Edu.loadItem")

  @JvmStatic
  val MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addMixIns(mapper)

    mapper
  }

  @VisibleForTesting
  val REMOTE_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addRemoteMixIns(mapper)

    mapper
  }

  @VisibleForTesting
  @JvmStatic
  val STUDENT_MAPPER: ObjectMapper by lazy {
    val mapper = createMapper()
    addMixIns(mapper)
    mapper.addMixIn(TaskFile::class.java, StudentTaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentAnswerPlaceholderYamlMixin::class.java)
    mapper.addStudentMixIns()

    mapper
  }

  @VisibleForTesting
  @JvmStatic
  val STUDENT_MAPPER_WITH_ENCRYPTION: ObjectMapper by lazy {
    val mapper = createMapper()
    addMixIns(mapper)
    val aesKey = if (!isUnitTestMode) getAesKey() else TEST_AES_KEY
    mapper.registerModule(EncryptionModule(aesKey))

    mapper.addMixIn(TaskFile::class.java, StudentTaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, StudentEncryptedAnswerPlaceholderYamlMixin::class.java)
    mapper.addStudentMixIns()

    mapper
  }

  private fun createMapper(): ObjectMapper {
    val yamlFactory = YAMLFactory.builder()
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
      .build()

    return JsonMapper.builder(yamlFactory)
      .addModule(kotlinModule())
      .addModule(JavaTimeModule())
      .defaultLocale(Locale.ENGLISH)
      .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .serializationInclusion(JsonInclude.Include.NON_EMPTY)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .disable(MapperFeature.AUTO_DETECT_FIELDS, MapperFeature.AUTO_DETECT_GETTERS, MapperFeature.AUTO_DETECT_IS_GETTERS)
      .build()
  }

  private fun addMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(CodeforcesCourse::class.java, CodeforcesCourseYamlMixin::class.java)
    mapper.addMixIn(CourseraCourse::class.java, CourseraCourseYamlMixin::class.java)
    mapper.addMixIn(Course::class.java, CourseYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, SectionYamlMixin::class.java)
    mapper.addMixIn(StepikLesson::class.java, StepikLessonYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    mapper.addMixIn(FrameworkLesson::class.java, FrameworkLessonYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, TaskYamlMixin::class.java)
    mapper.addMixIn(ChoiceTask::class.java, ChoiceTaskYamlMixin::class.java)
    mapper.addMixIn(CodeTask::class.java, CodeTaskYamlMixin::class.java)
    mapper.addMixIn(ChoiceOption::class.java, ChoiceOptionYamlMixin::class.java)
    mapper.addMixIn(TaskFile::class.java, TaskFileYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderYamlMixin::class.java)
    mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyYamlMixin::class.java)
  }

  private fun addRemoteMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(EduCourse::class.java, EduCourseRemoteInfoYamlMixin::class.java)
    mapper.addMixIn(CodeforcesCourse::class.java, CodeforcesCourseRemoteInfoYamlMixin::class.java)
    mapper.addMixIn(Lesson::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(StepikLesson::class.java, StepikLessonRemoteYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(DataTask::class.java, RemoteDataTaskYamlMixin::class.java)
    mapper.addMixIn(DataTaskAttempt::class.java, DataTaskAttemptYamlMixin::class.java)
    mapper.addHyperskillMixins()
  }

  private fun ObjectMapper.addHyperskillMixins() {
    addMixIn(HyperskillCourse::class.java, HyperskillCourseMixin::class.java)
    addMixIn(HyperskillProject::class.java, HyperskillProjectMixin::class.java)
    addMixIn(HyperskillStage::class.java, HyperskillStageMixin::class.java)
    addMixIn(HyperskillTopic::class.java, HyperskillTopicMixin::class.java)
  }

  private fun ObjectMapper.addStudentMixIns() {
    addMixIn(Course::class.java, StudentCourseYamlMixin::class.java)

    addMixIn(CheckiOStation::class.java, CheckiOStationYamlMixin::class.java)
    addMixIn(FrameworkLesson::class.java, StudentFrameworkLessonYamlMixin::class.java)

    addMixIn(Task::class.java, StudentTaskYamlMixin::class.java)
    addMixIn(RemoteEduTask::class.java, RemoteEduTaskYamlMixin::class.java)
    addMixIn(TheoryTask::class.java, TheoryTaskYamlUtil::class.java)
    addMixIn(ChoiceTask::class.java, StudentChoiceTaskYamlMixin::class.java)
    addMixIn(CheckiOMission::class.java, CheckiOMissionYamlMixin::class.java)
    addMixIn(CodeforcesTask::class.java, CodeforcesTaskYamlMixin::class.java)
    addMixIn(CodeforcesTaskWithFileIO::class.java, CodeforcesTaskWithFileIOYamlMixin::class.java)
    addMixIn(SortingTask::class.java, SortingTaskYamlMixin::class.java)
    addMixIn(MatchingTask::class.java, MatchingTaskYamlMixin::class.java)
    addMixIn(AnswerPlaceholder.MyInitialState::class.java, InitialStateMixin::class.java)
    addMixIn(CheckFeedback::class.java, FeedbackYamlMixin::class.java)
  }

  @JvmStatic
  fun saveAll(project: Project) {
    @NonNls
    val errorMessageToLog = "Attempt to create config files for project without course"
    val course = StudyTaskManager.getInstance(project).course ?: error(errorMessageToLog)
    val mapper = course.mapper
    saveItem(course, mapper)
    course.visitSections { section -> saveItem(section, mapper) }
    course.visitLessons { lesson ->
      lesson.visitTasks { task ->
        saveItem(task, mapper)
      }
      saveItem(lesson, mapper)
    }

    saveRemoteInfo(course)
  }

  @JvmOverloads
  @JvmStatic
  fun saveItem(item: StudyItem, mapper: ObjectMapper = item.course.mapper, configName: String = item.configFileName) {
    val course = item.course

    @NonNls
    val errorMessageToLog = "Failed to find project for course"
    val project = course.project ?: error(errorMessageToLog)
    if (!YamlFormatSettings.shouldCreateConfigFiles(project)) {
      return
    }
    item.saveConfig(project, configName, mapper)
  }

  @JvmStatic
  fun saveRemoteInfo(item: StudyItem) {
    when (item) {
      is ItemContainer -> {
        saveItemRemoteInfo(item)
        item.items.forEach { saveRemoteInfo(it) }
      }
      is Task -> {
        saveItemRemoteInfo(item)
      }
    }
  }

  @JvmStatic
  fun saveItemWithRemoteInfo(item: StudyItem) {
    saveItem(item)
    saveRemoteInfo(item)
  }

  @JvmStatic
  private fun saveItemRemoteInfo(item: StudyItem) {
    // we don't want to create remote info files in local courses
    if (item.id > 0 || item is HyperskillCourse) {
      saveItem(item, REMOTE_MAPPER, item.remoteConfigFileName)
    }
  }

  @JvmStatic
  fun startSynchronization(project: Project) {
    if (isUnitTestMode) {
      return
    }

    val disposable = StudyTaskManager.getInstance(project)
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(YamlSynchronizationListener(project), disposable)
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (isLocalConfigFile(file)) {
          if (project.isStudentProject()) {
            @NonNls
            val errorMessageToLog = "Can't find editor for a file: ${file.name}"
            val editor = file.getEditor(project) ?: error(errorMessageToLog)
            showNoEditingNotification(editor)
            return
          }

          // load item to show editor notification if config file is invalid
          YamlLoader.loadItem(project, file, false)
        }
      }
    })
  }

  private fun showNoEditingNotification(editor: Editor) {
    val label = JLabel(EduCoreBundle.message("yaml.editor.notification.configuration.file"))
    label.border = JBUI.Borders.empty(5, 10, 5, 0)

    val panel = JPanel(BorderLayout())
    panel.add(label, BorderLayout.CENTER)
    panel.background = MessageType.WARNING.popupBackground

    editor.headerComponent = panel
  }

  private fun StudyItem.saveConfig(project: Project, configName: String, mapper: ObjectMapper) {
    val dir = getConfigDir(project)

    invokeLater {
      runWriteAction {
        val file = dir.findOrCreateChildData(javaClass, configName)
        try {
          file.putUserData(LOAD_FROM_CONFIG, false)
          if (FileTypeManager.getInstance().getFileTypeByFile(file) == UnknownFileType.INSTANCE) {
            @NonNls
            val errorMessageToLog = "Failed to get extension for file ${file.name}"
            FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE,
              file.extension ?: error(errorMessageToLog))
          }
          VfsUtil.saveText(file, mapper.writeValueAsString(this))
        }
        finally {
          file.putUserData(LOAD_FROM_CONFIG, true)
        }
      }
    }
  }

  @JvmStatic
  fun isConfigFile(file: VirtualFile): Boolean {
    return isLocalConfigFile(file) || isRemoteConfigFile(file)
  }

  @JvmStatic
  fun isRemoteConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return REMOTE_COURSE_CONFIG == name || REMOTE_SECTION_CONFIG == name || REMOTE_LESSON_CONFIG == name || REMOTE_TASK_CONFIG == name
  }

  @JvmStatic
  fun isLocalConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return COURSE_CONFIG == name || SECTION_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name
  }

  val Course.mapper: ObjectMapper
    get() = if (isStudy) {
      if (isMarketplace) STUDENT_MAPPER_WITH_ENCRYPTION else STUDENT_MAPPER
    }
    else {
      MAPPER
    }
}

val StudyItem.configFileName: String
  get() = when (this) {
    is Course -> COURSE_CONFIG
    is Section -> SECTION_CONFIG
    is Lesson -> LESSON_CONFIG
    is Task -> TASK_CONFIG
    else -> {
      @NonNls
      val errorMessageToLog = "Unknown StudyItem type: ${javaClass.simpleName}"
      error(errorMessageToLog)
    }
  }

val StudyItem.remoteConfigFileName: String
  get() = when (this) {
    is Course -> REMOTE_COURSE_CONFIG
    is Section -> REMOTE_SECTION_CONFIG
    is Lesson -> REMOTE_LESSON_CONFIG
    is Task -> REMOTE_TASK_CONFIG
    else -> {
      @NonNls
      val errorMessageToLog = "Unknown StudyItem type: ${javaClass.simpleName}"
      error(errorMessageToLog)
    }
  }

fun StudyItem.getConfigDir(project: Project): VirtualFile {
  return if (this is Task && lesson is FrameworkLesson) {
    @NonNls
    val errorMessageToLog = "Config for '$name' task dir in framework lesson not found"
    lesson.getDir(project.courseDir)?.findChild(name) ?: error(errorMessageToLog)
  }
  else {
    getDir(project.courseDir) ?: error("Config for '$this' not found")
  }
}
