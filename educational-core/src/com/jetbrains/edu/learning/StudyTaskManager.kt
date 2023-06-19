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
import com.jetbrains.edu.learning.authorContentsStorage.fileContentsFromProjectAuthorContentsStorage
import com.jetbrains.edu.learning.authorContentsStorage.zip.UpdatableZipAuthorContentsStorage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.visitEduFiles
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.startSynchronization

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
  val authorContentsStorage = UpdatableZipAuthorContentsStorage(project.courseDir)

  /**
   * This method saves all current edu file contents to the author contents storage,
   * and reassigns edu file contents to point to this storage.
   * We may not reassign edu file contents to point on the storage, but we do it to free up some resources,
   * for example, if some file contents are pointing into memory.
   */
  @Transient
  fun updateAuthorContentsStorageAndTaskFileContents() {
    val course = course
    course ?: return

    authorContentsStorage.update(course)
    course.visitEduFiles { eduFile ->
      eduFile.contents = fileContentsFromProjectAuthorContentsStorage(eduFile)
    }
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
  }
}
