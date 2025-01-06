package com.jetbrains.edu.learning.newproject

import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.TrustedPaths
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.NOTIFICATIONS_SILENT_MODE
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.platform.util.progress.indeterminateStep
import com.intellij.platform.util.progress.progressStep
import com.intellij.platform.util.progress.withRawProgressReporter
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.util.PathUtil
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.messages.Topic
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.isLocalCourse
import com.jetbrains.edu.coursecreator.ui.CCOpenEducatorHelp
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.excludeFromArchive
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.CourseVisibility.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.isPreview
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ONLY_IDEA_DIRECTORY
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.unpackAdditionalFiles
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseProjectGenerator
import com.jetbrains.edu.learning.submissions.SubmissionSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * If you add any new public methods here, please do not forget to add it also to
 * @see HyperskillCourseProjectGenerator
 */
@Suppress("UnstableApiUsage")
abstract class CourseProjectGenerator<S : EduProjectSettings>(
  protected val courseBuilder: EduCourseBuilder<S>,
  protected val course: Course
) {

  @RequiresBlockingContext
  open fun afterProjectGenerated(project: Project, projectSettings: S, onConfigurationFinished: () -> Unit) {
    // project.isLocalCourse info is stored in PropertiesComponent to keep it after course restart on purpose
    // not to show login widget for local course
    project.isLocalCourse = course.isLocal

    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateAllWidgets()

    setUpPluginDependencies(project, course)

    if (!SubmissionSettings.getInstance(project).stateOnClose) {
      NavigationUtils.openFirstTask(course, project)
    }

    YamlFormatSynchronizer.saveAll(project)
    YamlFormatSynchronizer.startSynchronization(project)

    if (!course.isStudy && !course.isPreview && !isUnitTestMode) {
      CCOpenEducatorHelp.doOpen(project)
    }

    onConfigurationFinished()
  }

  // 'projectSettings' must have S type but due to some reasons:
  //  * We don't know generic parameter of EduPluginConfigurator after it was gotten through extension point mechanism
  //  * Kotlin and Java do type erasure a little differently
  // we use Object instead of S and cast to S when it needed
  @RequiresEdt
  @RequiresBlockingContext
  fun doCreateCourseProject(location: String, projectSettings: EduProjectSettings, initialLessonProducer: () -> Lesson = ::Lesson): Project? {
    return runWithModalProgressBlocking(
      ModalTaskOwner.guess(),
      EduCoreBundle.message("generate.course.progress.title"),
      TaskCancellation.cancellable()
    ) {
      doCreateCourseProjectAsync(location, projectSettings, initialLessonProducer)
    }
  }

  private suspend fun doCreateCourseProjectAsync(location: String, projectSettings: EduProjectSettings, initialLessonProducer: () -> Lesson): Project? {
    @Suppress("UNCHECKED_CAST")
    val castedProjectSettings = projectSettings as S
    applySettings(castedProjectSettings)
    val createdProject = createProject(location, initialLessonProducer) ?: return null

    withContext(Dispatchers.EDT) {
      blockingContext {
        afterProjectGenerated(createdProject, castedProjectSettings) {
          ApplicationManager.getApplication().messageBus
            .syncPublisher(COURSE_PROJECT_CONFIGURATION)
            .onCourseProjectConfigured(createdProject)
        }
      }
    }
    return createdProject
  }

  /**
   * Applies necessary changes to [course] object before course creation
   */
  protected open fun applySettings(projectSettings: S) {}

  /**
   * Create new project in given location.
   * To create course structure: modules, folders, files, etc. use [CourseProjectGenerator.createCourseStructure]
   *
   * @param locationString location of new project
   *
   * @return project of new course or null if new project can't be created
   */
  private suspend fun createProject(locationString: String, initialLessonProducer: () -> Lesson): Project? {
    val location = File(FileUtil.toSystemDependentName(locationString))
    val projectDirectoryExists = withContext(Dispatchers.IO) {
      location.exists() || location.mkdirs()
    }
    if (!projectDirectoryExists) {
      val message = ActionsBundle.message("action.NewDirectoryProject.cannot.create.dir", location.absolutePath)
      withContext(Dispatchers.EDT) {
        Messages.showErrorDialog(message, ActionsBundle.message("action.NewDirectoryProject.title"))
      }
      return null
    }
    val baseDir = blockingContext {
      LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location)
    }
    if (baseDir == null) {
      LOG.error("Couldn't find '$location' in VFS")
      return null
    }
    blockingContext {
      VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    }

    RecentProjectsManager.getInstance().lastProjectCreationLocation = PathUtil.toSystemIndependentName(location.parent)

    baseDir.putUserData(COURSE_MODE_TO_CREATE, course.courseMode)

    if (isCourseTrusted(course, isNewCourseCreatorCourse)) {
      @Suppress("UnstableApiUsage")
      TrustedPaths.getInstance().setProjectPathTrusted(location.toPath(), true)
    }

    val holder = CourseInfoHolder.fromCourse(course, baseDir)

    // If a course doesn't contain top-level items, let's count course itself as single item for creation.
    // It's a minor workaround to avoid zero end progress during course structure creation.
    val itemsToCreate = maxOf(1, course.items.size)
    // Total progress: item count steps for each top-level item plus one step for project creation itself
    val structureGenerationEndFraction = itemsToCreate.toDouble() / (itemsToCreate + 1)
    progressStep(structureGenerationEndFraction, EduCoreBundle.message("generate.course.structure.progress.text")) {
      createCourseStructure(holder, initialLessonProducer)
    }

    val newProject = progressStep(1.0, EduCoreBundle.message("generate.course.project.progress.text")) {
      openNewCourseProject(location.toPath(), this@CourseProjectGenerator::prepareToOpen)
    } ?: return null

    // A new progress window is needed because here we already have a new project frame,
    // and previous progress is not visible for user anymore
    withModalProgress(
      ModalTaskOwner.project(newProject),
      EduCoreBundle.message("generate.course.progress.title"),
      TaskCancellation.nonCancellable()
    ) {
      indeterminateStep(EduCoreBundle.message("generate.project.unpack.course.project.settings.progress.text")) {
        blockingContext {
          unpackAdditionalFiles(holder, ONLY_IDEA_DIRECTORY)
        }
      }
    }

    // after adding files with settings to .idea directory, almost all settings are synchronized automatically,
    // but the inspection profiles are to be synchronized manually
    ProjectInspectionProfileManager.getInstance(newProject).initializeComponent()

    return newProject
  }

  protected open suspend fun prepareToOpen(project: Project, module: Module) {
    NOTIFICATIONS_SILENT_MODE.set(project, true)
  }

  open fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler()

  open fun setUpProjectLocation(location: Path): Path = location

  private suspend fun openNewCourseProject(
    location: Path,
    prepareToOpenCallback: suspend (Project, Module) -> Unit,
  ): Project? {
    val beforeInitHandler = beforeInitHandler(location)
    val locationToOpen = setUpProjectLocation(location)
    val task = OpenProjectTask(course, prepareToOpenCallback, beforeInitHandler)

    return ProjectManagerEx.getInstanceEx().openProjectAsync(locationToOpen, task)
  }

  /**
   * Creates course structure in directory provided by [holder]
   */
  @VisibleForTesting
  open suspend fun createCourseStructure(holder: CourseInfoHolder<Course>, initialLessonProducer: () -> Lesson = ::Lesson) {
    holder.course.init(false)
    val isNewCourseCreatorCourse = isNewCourseCreatorCourse

    withRawProgressReporter {
      blockingContext {
        blockingContextToIndicator {
          if (isNewCourseCreatorCourse) {
            // `courseBuilder.createInitialLesson` is under blocking context with progress indicator as a temporary solution
            // to avoid deadlock during C++ course creation.
            // Otherwise, it may try to run a background process under modal progress during `CMAKE_MINIMUM_REQUIRED_LINE_VALUE` initialization.
            // See https://youtrack.jetbrains.com/issue/EDU-6702
            val lesson = courseBuilder.createInitialLesson(holder, initialLessonProducer)
            if (lesson != null) {
              course.addLesson(lesson)
            }
          }

          try {
            generateCourseContent(holder, ProgressManager.getInstance().progressIndicator)
          }
          catch (e: IOException) {
            LOG.error("Failed to generate course", e)
          }
        }
      }
    }
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  private fun generateCourseContent(
    holder: CourseInfoHolder<Course>,
    indicator: ProgressIndicator
  ) {
    measureTimeAndLog("Course content generation") {
      val course = holder.course
      if (!course.isStudy) {
        CCUtils.initializeCCPlaceholders(holder)
      }
      GeneratorUtils.createCourse(holder, indicator)
      if (course is EduCourse && course.isMarketplaceRemote && !course.isStudy) {
        checkIfAvailableOnRemote(course)
      }
      createAdditionalFiles(holder)
      EduCounterUsageCollector.eduProjectCreated(course)
    }
  }

  private fun checkIfAvailableOnRemote(course: EduCourse) {
    val remoteCourse = MarketplaceConnector.getInstance().searchCourse(course.id, course.isMarketplacePrivate)
    if (remoteCourse == null) {
      LOG.warn("Failed to get $MARKETPLACE course for imported from zip course with id: ${course.id}")
      LOG.info("Converting course to local. Course id: ${course.id}")
      course.convertToLocal()
    }
  }

  private fun addAdditionalFile(eduFile: EduFile) {
    val contains = course.additionalFiles.any { it.name == eduFile.name }

    if (contains) {
      course.additionalFiles = course.additionalFiles.map { if (it.name == eduFile.name) eduFile else it }
    }
    else {
      course.additionalFiles += eduFile
    }
  }

  /**
   * Creates additional files that are not in course object
   * The files are created on FS.
   * Some files that are intended to go into the course archive are also added to the [Course.additionalFiles].
   *
   * The default implementation takes the list of files from [autoCreatedAdditionalFiles], writes them to the FS and augments the list
   * [Course.additionalFiles] with those files that are not excluded by the [EduConfigurator].
   *
   * Consider overriding [autoCreatedAdditionalFiles] instead of this method, and generate the necessary additional files there.
   * Override this method only if it is impossible to generate additional files in-memory, and one needs to write them directly to FS.
   *
   * @param holder contains info about course project like root directory
   *
   * @throws IOException
   */
  @Throws(IOException::class)
  open fun createAdditionalFiles(holder: CourseInfoHolder<Course>) {
    for (file in autoCreatedAdditionalFiles(holder)) {
      val childFile = createChildFile(holder, holder.courseDir, file.name, file.contents) ?: continue

      if (course.configurator?.excludeFromArchive(holder, childFile) == false) {
        addAdditionalFile(file)
      }
    }
  }

  /**
   * Returns the list of additional files that must be added to the project.
   * Examines the FS and [Course.additionalFiles] to check, whether they lack some necessary files.
   */
  open fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> = emptyList()

  class BeforeInitHandler(val callback: (project: Project) -> Unit = { })

  private val isNewCourseCreatorCourse: Boolean
    get() = course.courseMode == CourseMode.EDUCATOR && course.items.isEmpty()

  companion object {
    private val LOG: Logger = Logger.getInstance(CourseProjectGenerator::class.java)

    val EDU_PROJECT_CREATED = Key.create<Boolean>("edu.projectCreated")

    val COURSE_MODE_TO_CREATE = Key.create<CourseMode>("edu.courseModeToCreate")

    @Topic.AppLevel
    val COURSE_PROJECT_CONFIGURATION: Topic<CourseProjectConfigurationListener> = createTopic("COURSE_PROJECT_CONFIGURATION")

    // TODO: provide more precise heuristic for Gradle, sbt and other "dangerous" build systems
    // See https://youtrack.jetbrains.com/issue/EDU-4182
    private fun isCourseTrusted(course: Course, isNewCourseCreatorCourse: Boolean): Boolean {
      if (isNewCourseCreatorCourse) return true
      if (course !is EduCourse) return true
      if (course.visibility is FeaturedVisibility) return true
      // Trust any course loaded from Marketplace since we verify every update
      // as well as show agreement for every course.
      //
      // Local visibility here means that the course was manually loaded from Marketplace
      // and opened via `Open Course from Disk` or similar action.
      // We can't be sure that it wasn't modified anyhow, so we don't trust such courses
      if (course.isMarketplaceRemote && course.visibility != LocalVisibility) return true
      return course.isPreview
    }

    fun OpenProjectTask(
      course: Course,
      prepareToOpenCallback: suspend (Project, Module) -> Unit,
      beforeInitHandler: BeforeInitHandler
    ): OpenProjectTask {
      return OpenProjectTask {
        forceOpenInNewFrame = true
        isNewProject = true
        isProjectCreatedWithWizard = true
        runConfigurators = true
        projectName = course.name
        beforeInit = {
          it.putUserData(EDU_PROJECT_CREATED, true)
          beforeInitHandler.callback(it)
        }
        preparedToOpen = {
          StudyTaskManager.getInstance(it.project).course = course
          prepareToOpenCallback(it.project, it)
        }
      }
    }
  }

  fun interface CourseProjectConfigurationListener {
    fun onCourseProjectConfigured(project: Project)
  }
}
