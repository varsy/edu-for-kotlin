package com.jetbrains.edu.learning;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.util.messages.Topic;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.authorContentsStorage.zip.ZipAuthorContentsStorage;
import com.jetbrains.edu.learning.courseFormat.Course;
import com.jetbrains.edu.learning.courseFormat.authorContentsStorage.AuthorContentsStorage;
import com.jetbrains.edu.learning.yaml.YamlDeepLoader;
import com.jetbrains.edu.learning.yaml.YamlFormatSettings;
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer;
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

  @Transient @Nullable
  private AuthorContentsStorage myAuthorContentsStorage;

  public StudyTaskManager(@Nullable Project project) {
    myProject = project;
  }

  public StudyTaskManager() {
    this(null);
  }

  @Transient
  public void setCourse(Course course) {
    myCourse = course;
    if (myProject != null) {
      myAuthorContentsStorage = ZipAuthorContentsStorage.initAuthorContentsStorage(course, getCourseDir(myProject));
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
