package com.jetbrains.edu.coursecreator.archive

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.intellij.externalDependencies.DependencyOnPlugin
import com.intellij.externalDependencies.ExternalDependenciesManager
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.concurrency.ThreadingAssertions
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.coursecreator.CCUtils.saveOpenedDocuments
import com.jetbrains.edu.coursecreator.actions.BinaryContentsFromDisk
import com.jetbrains.edu.coursecreator.actions.TextualContentsFromDisk
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.cipher.Cipher
import com.jetbrains.edu.learning.configuration.excludeFromArchive
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSE_META_FILE
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.exceptions.HugeBinaryFileException
import com.jetbrains.edu.learning.json.addStudyItemMixins
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.json.mixins.*
import com.jetbrains.edu.learning.json.pathInArchive
import com.jetbrains.edu.learning.json.setDateFormat
import com.jetbrains.edu.learning.marketplace.StudyItemIdGenerator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.TASK_CONFIG
import org.jetbrains.annotations.Nls
import java.io.*
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.text.Charsets.UTF_8

class CourseArchiveCreator(
  private val project: Project,
  private val outputProducer: CourseArchiveOutputProducer,
  private val cipher: Cipher = Cipher()
) {

  constructor(
    project: Project,
    location: Path,
    cipher: Cipher = Cipher()
  ) : this(project, CourseArchiveOutputProducer(location), cipher)

  /**
   * Returns `null` when course archive was created successfully, [Error] object otherwise
   */
  @RequiresEdt
  fun createArchive(course: Course): Error? {
    require(project.course == course) {
      "Given course is supposed to be associated with the current project"
    }

    ThreadingAssertions.assertEventDispatchThread()

    val result = prepareCourseCopy(course).
      flatMap { courseCopy -> generateArchive(courseCopy) }

    return when (result) {
      is Err -> {
        val error = result.error
        // TODO: separate error handling from course creation
        if (error is ExceptionError<*> && !isUnitTestMode) {
          LOG.error(error.exception)
        }
        error.immediateAction(project)
        error
      }
      is Ok -> null
    }
  }

  private fun prepareCourseCopy(course: Course): Result<Course, Error> {
    saveOpenedDocuments(project)

    if (course.isMarketplace) {
      ProgressManager.getInstance().runProcessWithProgressSynchronously({
        StudyItemIdGenerator.getInstance(project).generateIdsIfNeeded(course)
      }, EduCoreBundle.message("action.create.course.archive.progress.bar"), false, project)
    }

    course.updateEnvironmentSettings(project)
    val courseCopy = course.copy()

    return try {
      // We run prepareCourse() in EDT because it calls the VirtualFile.toStudentFile method that replaces placeholders inside the document.
      // Modifying the document requires a write action.
      prepareCourse(courseCopy)
      Ok(courseCopy)
    }
    catch (e: BrokenPlaceholderException) {
      Err(BrokenPlaceholderError(e))
    }
    catch (e: HugeBinaryFileException) {
      Err(HugeBinaryFileError(e))
    }
    catch (e: FileNotFoundException) {
      Err(AdditionalFileNotFoundError(e))
    }
    catch (e: Throwable) {
      if (e is ProcessCanceledException) throw e
      Err(OtherError(e))
    }
  }

  private fun generateArchive(course: Course): Result<Unit, Error> {
    val courseArchiveIndicator = CourseArchiveIndicator()
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<Result<Unit, Error>, RuntimeException>({
      try {
        unwrapExceptionCause {
          measureTimeAndLog("Create course archive") {
            doCreateCourseArchive(courseArchiveIndicator, course)
          }
        }

        Ok(Unit)
      }
      catch (e: HugeBinaryFileException) {
        Err(HugeBinaryFileError(e))
      }
      catch (e: IOException) {
        val baseMessage = EduCoreBundle.message("error.failed.to.generate.course.archive.io.exception")
        val exceptionMessage = e.message

        val message = if (exceptionMessage.isNullOrEmpty()) {
          baseMessage
        }
        else {
          baseMessage + " " + EduCoreBundle.message("error.failed.to.generate.course.archive.io.exception.additional.message", exceptionMessage)
        }
        Err(OtherError(e, message))
      }
      catch (e: Throwable) {
        if (e is ProcessCanceledException) throw e
        Err(OtherError(e))
      }
    }, EduCoreBundle.message("action.create.course.archive.progress.bar"), true, project)
  }

  /**
   * Creates course archive and writes it into [CourseArchiveOutputProducer.createOutput] produced by given [outputProducer].
   * Then closes the output stream.
   */
  private fun doCreateCourseArchive(courseArchiveIndicator: CourseArchiveIndicator, courseCopy: Course) {
    val courseDir = project.courseDir
    courseArchiveIndicator.init(courseDir, courseCopy, ProgressManager.getInstance().progressIndicator)

    ZipOutputStream(outputProducer.createOutput()).use { outputStream ->
      outputStream.withNewEntry(COURSE_META_FILE) {
        val writer = OutputStreamWriter(outputStream, UTF_8)
        generateJson(writer, courseCopy)
      }

      writeAllFilesToArchive(courseCopy, outputStream, courseArchiveIndicator)

      writeIconToArchive(outputStream)
    }

    synchronize(project)
  }

  private fun writeIconToArchive(outputStream: ZipOutputStream) {
    val iconFile = project.courseDir.findChild(EduFormatNames.COURSE_ICON_FILE)
    iconFile?.inputStream?.use { iconInputStream ->
      outputStream.withNewEntry(EduFormatNames.COURSE_ICON_FILE) {
        iconInputStream.copyTo(outputStream)
      }
    }
  }

  private fun writeAllFilesToArchive(
    courseCopy: Course,
    outputStream: ZipOutputStream,
    courseArchiveIndicator: CourseArchiveIndicator
  ) {
    courseCopy.visitEduFiles { eduFile ->
      outputStream.withNewEntry(eduFile.pathInArchive) {
        val bytes = when (val contents = eduFile.contents) {
          is BinaryContents -> contents.bytes
          is TextualContents -> contents.text.toByteArray(UTF_8)
          is UndeterminedContents -> throw IllegalStateException("All contents must be disambiguated before writing archive")
        }
        outputStream.write(cipher.encrypt(bytes))
        courseArchiveIndicator.writeFile(eduFile)
      }
    }
  }

  @Throws(IOException::class)
  private fun ZipOutputStream.withNewEntry(name: String, action: () -> Unit) {
    try {
      putNextEntry(ZipEntry(name))
      action()
    }
    finally {
      closeEntry()
    }
  }

  private fun prepareCourse(course: Course) {
    loadActualTextsForTasks(project, course)
    prepareAdditionalFiles(project, course)
    course.sortItems()
    course.pluginDependencies = collectCourseDependencies(project, course)
    course.courseMode = CourseMode.STUDENT
  }

  private fun synchronize(project: Project) {
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
    ProjectView.getInstance(project).refresh()
  }

  private fun generateJson(out: Writer, course: Course) {
    val indenter = DefaultIndenter("  ", "\n")
    val prettyPrinter = DefaultPrettyPrinter()
    prettyPrinter.indentArraysWith(indenter)
    prettyPrinter.indentObjectsWith(indenter)

    val mapper = getMapper(course)
    mapper.writer(prettyPrinter).writeValue(out, course)
  }

  private fun getMapper(course: Course): ObjectMapper {
    val module = SimpleModule()
      .addSerializer(EduCourse::class.java, EduCoursePluginVersionSerializer())

    val mapper = JsonMapper.builder()
      .addModule(module)
      .addModule(EncryptionModule(cipher))
      .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false)
      .commonMapperSetup(course)
      .setDateFormat()
      .build()
    mapper.addStudyItemMixins()

    return mapper
  }

  /**
   * Sometimes when a user cancels a process, the ProcessCanceledException becomes a cause of another exception.
   * We need to get rid of the caused exception to see that the process was actually cancelled.
   *
   * Another case is the JsonMappingException, it wraps other exceptions that were a real cause and are interesting for us.
   */
  private fun unwrapExceptionCause(action: () -> Unit) = try {
    action()
  }
  catch (e: JsonMappingException) {
    throw e.cause ?: e
  }
  catch (e: Exception) {
    val cause = e.cause
    if (cause is ProcessCanceledException) throw cause
    throw e
  }

  class EduCoursePluginVersionSerializer : JsonSerializer<EduCourse>() {
    override fun serialize(value: EduCourse, jgen: JsonGenerator, provider: SerializerProvider) {
      jgen.writeStartObject()
      val javaType = provider.constructType(value::class.java)
      val beanDesc: BeanDescription = provider.config.introspect(javaType)
      val serializer: JsonSerializer<Any> =
        BeanSerializerFactory.instance.findBeanOrAddOnSerializer(provider, javaType, beanDesc,
                                                                 provider.isEnabled(MapperFeature.USE_STATIC_TYPING))
      serializer.unwrappingSerializer(null).serialize(value, jgen, provider)
      jgen.addPluginVersion()
      jgen.writeEndObject()
    }

    private fun JsonGenerator.addPluginVersion() {
      val fieldName = JsonMixinNames.PLUGIN_VERSION
      val pluginVersion = if (isUnitTestMode) TEST_PLUGIN_VERSION else pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"
      writeObjectField(fieldName, pluginVersion)
    }
  }

  companion object {
    private val LOG = logger<CourseArchiveCreator>()
    private const val TEST_PLUGIN_VERSION = "yyyy.2-yyyy.1-TEST"

    fun JsonMapper.Builder.commonMapperSetup(course: Course): JsonMapper.Builder {
      if (course is CourseraCourse) {
        addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderMixin::class.java)
      }
      else {
        addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
      }
      addMixIn(PluginInfo::class.java, PluginInfoMixin::class.java)
      addMixIn(EduFile::class.java, EduFileMixin::class.java)
      addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
      addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
      disable(MapperFeature.AUTO_DETECT_FIELDS)
      disable(MapperFeature.AUTO_DETECT_GETTERS)
      disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
      return this
    }

    private fun loadActualTextsForTasks(project: Project, course: Course) {
      val courseDir = project.courseDir

      course.visitLessons { lesson ->
        val lessonDir = lesson.getDir(courseDir)
        if (lessonDir == null) return@visitLessons
        for (task in lesson.taskList) {
          loadActualTexts(project, task)
        }
      }
    }

    /**
     * Loads actual texts for additional files and filters out files that are not allowed to be additional, such as yaml configurations,
     * files inside tasks, etc.
     */
    private fun prepareAdditionalFiles(project: Project, course: Course) {
      val courseDir = project.courseDir

      val configurator = course.configurator ?: return
      val courseInfoHolder = project.toCourseInfoHolder()
      val filteredAdditionalFiles = mutableListOf<EduFile>()

      for (additionalFile in course.additionalFiles) {
        val fsFile = courseDir.findFileByRelativePath(additionalFile.name)
        if (fsFile == null) {
          throw FileNotFoundException(EduCoreBundle.message("error.additional.file.does.not.exist", additionalFile.name))
        }

        if (
          !configurator.excludeFromArchive(courseInfoHolder, fsFile) && // TODO: EDU-7821
          fsFile.getContainingTask(courseInfoHolder) == null
        ) {
          filteredAdditionalFiles += additionalFile
          loadActualText(additionalFile, fsFile)
        }
      }

      course.additionalFiles = filteredAdditionalFiles
    }

    private fun loadActualTexts(project: Project, task: Task) {
      val taskDir = task.getDir(project.courseDir) ?: return
      convertToStudentTaskFiles(project, task, taskDir)
      task.updateDescriptionTextAndFormat(project)
    }

    private fun loadActualText(additionalFile: EduFile, fsFile: VirtualFile) {
      additionalFile.contents = when(additionalFile.isBinary) {
        false -> TextualContentsFromDisk(fsFile)
        true -> BinaryContentsFromDisk(fsFile)
        // Undetermined contents seem impossible because additional files
        // are created and read from course-info.yaml always with determined contents.
        // But if the contents are anyhow undetermined (probably because of tests), we must disambiguate it.
        null -> if (fsFile.isToEncodeContent) {
          BinaryContentsFromDisk(fsFile)
        }
        else {
          TextualContentsFromDisk(fsFile)
        }
      }
    }

    private fun convertToStudentTaskFiles(project: Project, task: Task, taskDir: VirtualFile) {
      val studentTaskFiles = LinkedHashMap<String, TaskFile>()
      for ((key, value) in task.taskFiles) {
        val answerFile = value.findTaskFileInDir(taskDir) ?: continue

        val studentFile = answerFile.toStudentFile(project, task)
        if (studentFile != null) {
          studentTaskFiles[key] = studentFile
        }
      }
      task.taskFiles = studentTaskFiles
    }

    private fun collectCourseDependencies(project: Project, course: Course): List<PluginInfo> {
      val requiredPluginIds = course.compatibilityProvider?.requiredPlugins().orEmpty().mapTo(HashSet()) { it.stringId }
      return ExternalDependenciesManager.getInstance(project).getDependencies(DependencyOnPlugin::class.java)
        // Compatibility provider may produce different required plugins in different IDEs,
        // for example, `PythonCore` in IDEA Community and `Pythonid` in PyCharm Pro.
        // So exclude them from the plugin archive to keep course compatible with all desired IDEs.
        // Note, it doesn't allow user to start course without required plugins because
        // we still check required plugins from compatibility providers on course creation.
        // See https://youtrack.jetbrains.com/issue/EDU-4514
        .filter { it.pluginId !in requiredPluginIds }
        .map {
          PluginInfo(it.pluginId,
                     PluginManager.getInstance().findEnabledPlugin(PluginId.getId(it.pluginId))?.name ?: it.pluginId,
                     it.minVersion,
                     it.maxVersion)
        }
    }
  }

  sealed class Error {

    abstract val message: @Nls String

    /**
     * Action which is supposed to be performed without additional user actions
     */
    @RequiresEdt
    open fun immediateAction(project: Project) {}
  }

  abstract class ExceptionError<T : Throwable>(val exception: T) : Error() {
    override val message: String
      get() = exception.message.orEmpty()
  }

  class HugeBinaryFileError(e: HugeBinaryFileException) : ExceptionError<HugeBinaryFileException>(e)
  class BrokenPlaceholderError(e: BrokenPlaceholderException) : ExceptionError<BrokenPlaceholderException>(e) {
    override fun immediateAction(project: Project) {
      val yamlFile = exception.placeholder.taskFile.task.getDir(project.courseDir)?.findChild(TASK_CONFIG) ?: return
      FileEditorManager.getInstance(project).openFile(yamlFile, true)
    }
  }
  // TODO: use more specific exception for error related to additional files.
  //  `FileNotFoundException` is not related to additional files
  //  and in theory may occur in other cases as well
  class AdditionalFileNotFoundError(e: FileNotFoundException) : ExceptionError<FileNotFoundException>(e)
  class OtherError(e: Throwable, private val errorMessage: @Nls String? = null) : ExceptionError<Throwable>(e) {
    override val message: String
      get() = errorMessage ?: EduCoreBundle.message("error.failed.to.create.course.archive")
  }
}
