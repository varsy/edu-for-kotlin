package com.jetbrains.edu.learning

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.project.stateStore
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.Transient
import com.jetbrains.edu.learning.authorContentsStorage.sqlite.SQLiteAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.pathInAuthorContentsStorageForEduFile
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
@State(name = "StudySettings", storages = [Storage(value = "study_project.xml", roamingType = RoamingType.DISABLED)])
class StudyTaskManager(private val project: Project) : DumbAware, Disposable {
  @Volatile
  private var courseLoadedWithError = false

  @Transient
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

  /**
   * This method saves all current edu file contents to the author contents storage,
   * and reassigns edu file contents to point to this storage.
   * We may not reassign edu file contents to point on the storage, but we do it to free up some resources,
   * for example, if some file contents are pointing into memory.
   */
  @Transient
  fun updateAuthorContentsStorageAndTaskFileContents() {
    /*val course = course
    course ?: return

    //authorContentsStorage.update(course)
    course.visitEduFiles { eduFile ->
      eduFile.contentsHolder = authorContentsStorage.holderForPath(pathInAuthorContentsStorageForEduFile(eduFile))
    }*/
  }

  @get:Transient
  @set:Transient
  var course: Course?
    get() = _course
    set(course) {
      _course = course
      course?.apply {
        project.messageBus.syncPublisher(COURSE_SET).courseSet(this)
      }
    }

  override fun dispose() {}

  companion object {
    val COURSE_SET = Topic.create("Edu.courseSet", CourseSetListener::class.java)

    @JvmStatic
    fun getInstance(project: Project): StudyTaskManager {
      val manager = project.service<StudyTaskManager>()
      if (!project.isDefault && !LightEdit.owns(project) && manager.course == null
          && project.isEduYamlProject() && !manager.courseLoadedWithError) {
        manager.authorContentsStorage = SQLiteAuthorContentsStorage.openOrCreateDB(getZipPathForProjectDirectory(project))
        val course = ApplicationManager.getApplication().runReadAction(Computable { loadCourse(project) })
        manager.courseLoadedWithError = course == null
        if (course != null) {
          manager.course = course
        }
        startSynchronization(project)
      }
      return manager
    }

    const val COURSE_AUTHOR_CONTENTS_FILE = "author_contents_storage.db"
    private val courseDir2zipDir = mutableMapOf<VirtualFile, Path>()

    fun getZipPathForProjectDirectory(project: Project): Path {
      val courseStorageFileFolder = project.projectFile?.parent ?: throw IllegalStateException("working with a default project")

      // We act differently depending on whether courseStorageFileFolder is on a local file system or on a file system for tests
      val fileSystem = courseStorageFileFolder.fileSystem
      val courseStorageFileFolderPath = fileSystem.getNioPath(courseStorageFileFolder) // null for the test file system
      val realFolderForDBfile = courseStorageFileFolderPath ?: courseDir2zipDir.computeIfAbsent(courseStorageFileFolder) {
        Files.createTempDirectory("boundAuthorContentStorage")
      }
      return realFolderForDBfile.resolve(COURSE_AUTHOR_CONTENTS_FILE)
    }
  }
}
