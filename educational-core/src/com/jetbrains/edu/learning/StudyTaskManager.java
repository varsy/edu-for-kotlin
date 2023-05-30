package com.jetbrains.edu.learning;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.authorContentsStorage.AuthorContentsStorageUtilsKt;
import com.jetbrains.edu.learning.authorContentsStorage.zip.UpdatableZipAuthorContentsStorage;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage;
import com.jetbrains.edu.learning.courseFormat.ext.CourseExt;
import com.jetbrains.edu.learning.yaml.YamlDeepLoader;
import com.jetbrains.edu.learning.yaml.YamlFormatSettings;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
import kotlin.Unit;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.jetbrains.edu.learning.OpenApiExtKt.getCourseDir;

/**
 * Implementation of class which contains all the information
 * about study in context of current project
 */
@State(name = "StudySettings", storages = @Storage(value = "study_project.xml", roamingType = RoamingType.DISABLED))
public class StudyTaskManager implements PersistentStateComponent<Element>, DumbAware, Disposable {
  public static final Topic<CourseSetListener> COURSE_SET = Topic.create("Edu.courseSet", CourseSetListener.class);
  private volatile boolean courseLoadedWithError = false;

  @Transient
  private Course myCourse;

  @Transient @Nullable private final Project myProject;

  /**
   * This is the author contents storage used to store all the edu files contents for the current course.
   * It is updatable, that is, if the course is updated by the student, this storage also updates.
   *
   * This storage is needed only in the student mode.
   * It should not be used in the course creation mode.
   */
  @Transient @Nullable
  private final UpdatableZipAuthorContentsStorage myAuthorContentsStorage;

  public StudyTaskManager(@Nullable Project project) {
    myProject = project;

    if (myProject != null) {
      VirtualFile courseDir = getCourseDir(project);
      myAuthorContentsStorage = new UpdatableZipAuthorContentsStorage(courseDir);
    } else {
      myAuthorContentsStorage = null;
    }
  }

  public StudyTaskManager() {
    this(null);
  }

  @Transient
  public void setCourse(Course course) {
    myCourse = course;
    if (myProject != null) {
      myProject.getMessageBus().syncPublisher(COURSE_SET).courseSet(course);
    }
  }

  @Nullable
  @Transient
  public Course getCourse() {
    return myCourse;
  }

  @Nullable
  @Override
  public Element getState() {
    return null;
  }

  @Nullable
  @Transient
  public AuthorContentsStorage getAuthorContentsStorage() {
    return myAuthorContentsStorage;
  }

  /**
   * This method saves all current edu file contents to the author contents storage,
   * and reassigns edu file contents to point to this storage.
   * We may not reassign edu file contents to point on the storage, but we do it to free up some resources,
   * for example, if some file contents are pointing into memory.
   */
  @Transient
  public void updateAuthorContentsStorageAndTaskFileContents() {
    if (myAuthorContentsStorage != null)
      myAuthorContentsStorage.update(myCourse);

    CourseExt.visitEduFiles(myCourse, eduFile -> {
      eduFile.setContents(AuthorContentsStorageUtilsKt.fileContentsFromProjectAuthorContentsStorage(eduFile));
      return Unit.INSTANCE;
    });
  }

  @Override
  public void loadState(@NotNull Element state) {
  }

  @Override
  public void dispose() { }

  public static StudyTaskManager getInstance(@NotNull final Project project) {
    StudyTaskManager manager = ServiceManager.getService(project, StudyTaskManager.class);
    if (!project.isDefault() &&
        !LightEdit.owns(project) &&
        manager != null &&
        manager.getCourse() == null &&
        YamlFormatSettings.isEduYamlProject(project) &&
        !manager.courseLoadedWithError) {
      Course course = ApplicationManager.getApplication().runReadAction((Computable<Course>)() -> YamlDeepLoader.loadCourse(project));
      manager.courseLoadedWithError = course == null;
      if (course != null) {
        manager.setCourse(course);
      }
      YamlFormatSynchronizer.startSynchronization(project);
    }
    return manager;
  }
}
