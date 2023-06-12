package com.jetbrains.edu.learning

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authorContentsStorage.sqlite.SQLiteAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.StorageAuthorContentsHolder
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.startSynchronization
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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
  lateinit var authorContentsStorage: AuthorContentsStorage
    private set

  @get:Transient
  @set:Transient
  var course: Course?
    get() = _course
    set(course) {
      _course = course
      authorContentsStorage = SQLiteAuthorContentsStorage.openOrCreateDB(getZipPathForProjectDirectory(project))

      updateCourseFileContentsHolders()
      course?.apply {
        project.messageBus.syncPublisher(COURSE_SET).courseSet(this)
      }
    }

  /**
   * Should be called each time the course is updated
   */
  fun updateCourseFileContentsHolders() {
    _course?.visitEduFiles { eduFile ->
      if (!contentsHolderIsForThisFileFromThisStorage(eduFile, project)) {
        val oldContents = eduFile.contents
        eduFile.contentsHolder = object : StorageAuthorContentsHolder(eduFile) {
          override val storage: AuthorContentsStorage
            get() = authorContentsStorage
        }
        eduFile.contents = oldContents
      }
    }
  }

  private fun contentsHolderIsForThisFileFromThisStorage(eduFile: EduFile, project: Project): Boolean {
    val contentsHolder = eduFile.contentsHolder
    if (contentsHolder !is StorageAuthorContentsHolder) return false

    if (contentsHolder.eduFile != eduFile) return false
    val storage = contentsHolder.storage
    if (storage !is SQLiteAuthorContentsStorage) return false
    return storage.db == getZipPathForProjectDirectory(project)
  }

  override fun dispose() {}

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

    private const val COURSE_AUTHOR_CONTENTS_FILE = "author_contents_storage.db"
    private val courseDir2zipDir = mutableMapOf<Path, Path>()

    private fun getZipPathForProjectDirectory(project: Project): Path {
      val projectFile = Paths.get(
        project.projectFilePath ?: throw IllegalStateException("working with a default project")
      )

      val courseStorageFileFolder = projectFile.parent
      val realFolderForDBfile = if (Files.exists(courseStorageFileFolder)) {
        courseStorageFileFolder
      }
      else {
        courseDir2zipDir.computeIfAbsent(courseStorageFileFolder) {
          Files.createTempDirectory("boundAuthorContentStorage")
        }
      }

      return realFolderForDBfile.resolve(COURSE_AUTHOR_CONTENTS_FILE)
    }
  }
}
