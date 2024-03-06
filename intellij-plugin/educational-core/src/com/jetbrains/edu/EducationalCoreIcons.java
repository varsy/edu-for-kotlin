package com.jetbrains.edu;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class EducationalCoreIcons {
  /**
   * Loads an icon using the default method.
   * This is currently the default way to load icons based on their path in the `resources` directory.
   * Note that there is also a method available for loading icons as rasterized images
   * using the {@link #loadRasterized(String)} method.
   * <p/>
   * However, the {@link #loadRasterized(String)} method has some limitations because it uses platform API that is not fully adjusted
   * for our needs and may not perfectly fit our use case, but it remains a necessary option for now.
   *
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@NotNull String path) {
    return IconLoader.getIcon(path, EducationalCoreIcons.class);
  }

  /**
   * Loads an icon in a modern and efficient manner as a rasterized image.
   * Currently, this method is used exclusively for loading tool window icons.
   * <p>
   * Note: The last two arguments passed to {@link IconManager#loadRasterizedIcon} are `cacheKey` and `flags`.
   * Both are set to 0, which is acceptable; however, we should consider using a different API in the future
   * that does not apply caching, which is currently unavailable
   *
   * @param path the path to the icon in the resources directory, without a leading slash.
   *             The path must be relative to the classpath root.
   * @return the loaded {@link Icon} object.
   * @throws IllegalArgumentException if the provided path starts with a leading slash.
   */
  private static @NotNull Icon loadRasterized(@NotNull String path) {
    if (path.startsWith("/")) {
      throw new IllegalArgumentException("Path must be specified without a leading slash");
    }

    return IconManager.getInstance().loadRasterizedIcon(path, EducationalCoreIcons.class.getClassLoader(), 0, 0);
  }

  /**
   * Utility class that provides icons for various actions
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Actions {
    public static final Icon ApplyCode = load("/icons/com/jetbrains/edu/learning/applyCode.svg");
    public static final Icon CommentTask = load("/icons/com/jetbrains/edu/learning/commentTask.svg");
    public static final Icon EduCourse = load("/icons/com/jetbrains/edu/eduCourseAction.svg");
    public static final Icon IgnoreSyncFile = load("/icons/com/jetbrains/edu/actions/syncFilesIgnore.svg");
    public static final Icon RateCourse = load("/icons/com/jetbrains/edu/learning/rateCourse.svg");
    public static final Icon ResetTask = load("/icons/com/jetbrains/edu/learning/resetTask.svg");
    public static final Icon SyncChanges = load("/icons/com/jetbrains/edu/actions/syncFiles.svg");
  }

  /**
   * Utility class that provides icons for the check panel.
   */
  public static final class CheckPanel {
    public static final Icon CheckDetailsToolWindow = loadRasterized("icons/com/jetbrains/edu/learning/checkDetailsToolWindow.svg");
    public static final Icon ResultCorrect = load("/icons/com/jetbrains/edu/learning/resultCorrect.svg");
    public static final Icon ResultIncorrect = load("/icons/com/jetbrains/edu/learning/resultIncorrect.svg");
  }

  /**
   * Utility class that provides icons for the course creator.
   */
  public static final class CourseCreator {
    public static final Icon GuidedProject = load("/icons/com/jetbrains/edu/courseCreator/guidedProject.svg");
    public static final Icon GuidedProjectSelected = load("/icons/com/jetbrains/edu/courseCreator/guidedProjectSelected.svg");
    public static final Icon SimpleLesson = load("/icons/com/jetbrains/edu/courseCreator/simpleLesson.svg");
    public static final Icon SimpleLessonSelected = load("/icons/com/jetbrains/edu/courseCreator/simpleLessonSelected.svg");
    public static final Icon NewLesson = load("/icons/com/jetbrains/edu/courseCreator/addLesson.svg");
    public static final Icon NewTask = load("/icons/com/jetbrains/edu/courseCreator/addTask.svg");
  }

  /**
   * Utility class that provides icons for various components in the Course View
   */
  public static final class CourseView {
    public static final Icon CourseTree = load("/icons/com/jetbrains/edu/eduCourseTree.svg");
    public static final Icon IdeTask = load("/icons/com/jetbrains/edu/eduTaskIdeDefault.svg");
    public static final Icon IdeTaskSolved = load("/icons/com/jetbrains/edu/eduTaskIdeDone.svg");
    public static final Icon Lesson = load("/icons/com/jetbrains/edu/eduLessonDefault.svg");
    public static final Icon LessonSolved = load("/icons/com/jetbrains/edu/eduLessonDone.svg");
    public static final Icon Section = load("/icons/com/jetbrains/edu/eduSectionDefault.svg");
    public static final Icon SectionSolved = load("/icons/com/jetbrains/edu/eduSectionDone.svg");
    public static final Icon SyncFilesModInfo = load("/icons/com/jetbrains/edu/syncFilesModInfo.svg");
    public static final Icon SyncFilesModWarning = load("/icons/com/jetbrains/edu/syncFilesModWarning.svg");
    public static final Icon Task = load("/icons/com/jetbrains/edu/eduTaskDefault.svg");
    public static final Icon TaskFailed = load("/icons/com/jetbrains/edu/eduTaskFailed.svg");
    public static final Icon TaskSolved = load("/icons/com/jetbrains/edu/eduTaskDone.svg");
    public static final Icon TheoryTask = load("/icons/com/jetbrains/edu/eduTaskTheoryDefault.svg");
    public static final Icon TheoryTaskSolved = load("/icons/com/jetbrains/edu/eduTaskTheoryDone.svg");
    public static final Icon UsersNumber = load("/icons/com/jetbrains/edu/usersNumber.svg");
  }

  /**
   * Utility class that provides icons for various programming languages
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Language {
    public static final Icon Android = load("/icons/com/jetbrains/edu/learning/AndroidLogo.svg");
    public static final Icon Cpp = load("/icons/com/jetbrains/edu/learning/CAndC++Logo.svg");
    public static final Icon CSharp = load("/icons/com/jetbrains/edu/learning/CSharpLogo.svg");
    public static final Icon Go = load("/icons/com/jetbrains/edu/learning/GoLogo.svg");
    public static final Icon Java = load("/icons/com/jetbrains/edu/learning/JavaLogo.svg");
    public static final Icon JavaScript = load("/icons/com/jetbrains/edu/learning/JavaScriptLogo.svg");
    public static final Icon Kotlin = load("/icons/com/jetbrains/edu/learning/KotlinLogo.svg");
    public static final Icon Php = load("/icons/com/jetbrains/edu/learning/PhpLogo.svg");
    public static final Icon Python = load("/icons/com/jetbrains/edu/learning/PythonLogo.svg");
    public static final Icon Rust = load("/icons/com/jetbrains/edu/learning/RustLogo.svg");
    public static final Icon Scala = load("/icons/com/jetbrains/edu/learning/ScalaLogo.svg");
    public static final Icon Shell = load("/icons/com/jetbrains/edu/learning/ShellLogo.svg");
  }

  /**
   * Utility class that provides icons for various educational platforms
   *
   * <p>All child icons are 16x16</p>
   */
  public static final class Platform {
    /**
     * Class that provides tab icons for different platforms
     *
     * <p>Tab icons are 24x24 and are supposed to be used only in implementations of
     * {@code com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory}</p>
     */
    public static final class Tab {
      public static final Icon CourseraTab = load("/icons/com/jetbrains/edu/learning/coursera.svg");
      public static final Icon JetBrainsAcademyTab = load("/icons/com/jetbrains/edu/learning/JB_academy_course_tab.svg");
      public static final Icon MarketplaceTab = load("/icons/com/jetbrains/edu/learning/marketplace_courses_tab.svg");
    }

    public static final Icon JetBrainsAcademy = load("/icons/com/jetbrains/edu/learning/JB_academy.svg");
  }

  /**
   * Utility class that provides icons for task submissions
   *
   * <p>All icons are 11x11</p>
   */
  public static final class Submission {
    public static final Icon TaskFailed = load("/icons/com/jetbrains/edu/submission/taskFailed@2x.png");
    public static final Icon TaskFailedHighContrast = load("/icons/com/jetbrains/edu/submission/taskFailedHighContrast@2x.png");
    public static final Icon TaskSolved = load("/icons/com/jetbrains/edu/submission/taskSolved@2x.png");
    public static final Icon TaskSolvedHighContrast = load("/icons/com/jetbrains/edu/submission/taskSolvedHighContrast@2x.png");
  }

  /**
   * Utility class that provides icons for the task tool window
   */
  public static final class TaskToolWindow {
    public static final Icon Clock = load("/icons/com/jetbrains/edu/learning/clock.svg");
    public static final Icon CourseToolWindow = loadRasterized("icons/com/jetbrains/edu/eduCourseTask.svg");
    public static final Icon MoveDown = load("/icons/com/jetbrains/edu/learning/moveDown.svg");
    public static final Icon MoveUp = load("/icons/com/jetbrains/edu/learning/moveUp.svg");
    public static final Icon NavigationMapTheoryTask = load("/icons/com/jetbrains/edu/eduNavigationMapTheoryTask.svg");
  }

  public static final Icon Report = load("/icons/com/jetbrains/edu/learning/report.svg"); // 14x14

  public static final Icon aiAssistant = load("/icons/com/jetbrains/edu/learning/aiAssistantToolWindow.svg");

  public static final Icon DOT = load("/icons/com/jetbrains/edu/learning/dot.svg"); // 3x3
}
