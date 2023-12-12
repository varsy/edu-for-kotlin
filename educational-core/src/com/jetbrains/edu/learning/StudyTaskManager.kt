package com.jetbrains.edu.learning

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authorContentsStorage.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.startSynchronization
import java.nio.file.Files
import java.nio.file.Path

/**
 * Implementation of class which contains all the information about study in context of current project
 */
@Service(Service.Level.PROJECT)
class StudyTaskManager(private val project: Project) : DumbAware, Disposable {
  @Volatile
  private var courseLoadedWithError = false

  private var _course: Course? = null

  /**
   * This is the author contents storage used to store all the edu files contents for the current course.
   * It is updatable, that is, if the course is updated by the student, this storage also updates.
   * This storage is needed only in the student mode.
   * It should not be used in the course creation mode.
   */
  @Transient
  lateinit var authorContentsStorage: SQLiteContentsStorage
    private set

  var course: Course?
    get() = _course
    set(course) {
      _course = course

      if (_course?.isStudy == true) {
        if (!this::authorContentsStorage.isInitialized) {
          authorContentsStorage = SQLiteContentsStorage.openOrCreateDB(getZipPathForProjectDirectory(project))
          Disposer.register(this, authorContentsStorage)
        }
        updateCourseFileContentsHolders()
      }

      course?.apply {
        project.messageBus.syncPublisher(COURSE_SET).courseSet(this)
      }
    }

  override fun dispose() {}

  /**
   * Should be called each time the course is updated
   */
  private fun updateCourseFileContentsHolders() {
    _course?.visitEduFiles { eduFile ->
      val contents = eduFile.contents
      if (contents is SQLiteContents && contents.storage == authorContentsStorage) {
        return@visitEduFiles
      }

      val path = if (eduFile is TaskFile) {
        eduFile.task.getPathInCourse() + "/"
      }
      else {
        ""
      } + eduFile.name

      eduFile.contents = when (contents) {
        is BinaryContents -> {
          authorContentsStorage.put(path, contents.bytes)
          SQLiteBinaryContents(authorContentsStorage, path)
        }
        is TextualContents -> {
          authorContentsStorage.put(path, contents.text)
          SQLiteTextualContents(authorContentsStorage, path)
        }
        is UndeterminedContents -> {
          authorContentsStorage.put(path, contents.textualRepresentation)
          SQLiteUndeterminedContents(authorContentsStorage, path)
        }
      }
    }
  }

  companion object {
    val COURSE_SET = Topic.create("Edu.courseSet", CourseSetListener::class.java)

    fun getInstance(project: Project): StudyTaskManager {
      val manager = project.service<StudyTaskManager>()
      if (!project.isDefault && !LightEdit.owns(project) && manager.course == null
          && project.isEduYamlProject() && !manager.courseLoadedWithError) {
        val course = ApplicationManager.getApplication().runReadAction(Computable { loadCourse(project) })
        manager.courseLoadedWithError = course == null
        if (course != null) {
          manager.course = course
        }
        startSynchronization(project)
      }
      return manager
    }

    const val COURSE_AUTHOR_CONTENTS_FILE = ".author_contents_storage_db"
    private val courseDir2zipDir = mutableMapOf<String, Path>()

    private fun getZipPathForProjectDirectory(project: Project): Path {
      val courseDir = project.courseDir
      val courseDirPath = LocalFileSystem.getInstance().getNioPath(courseDir) ?: // null, if the test mode
        courseDir2zipDir.computeIfAbsent(courseDir.path) {
          Files.createTempDirectory("boundAuthorContentStorage")
        }

      return courseDirPath.resolve(COURSE_AUTHOR_CONTENTS_FILE)
    }
  }
}
