package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListenerTest
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.yaml.format.TaskWithType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.YAML_TEST_THROW_EXCEPTION

abstract class VirtualFileListenerTestBase : EduTestCase() {
  protected abstract val courseMode: CourseMode
  protected abstract fun createListener(project: Project): EduVirtualFileListener

  override fun setUp() {
    super.setUp()
    ApplicationManager.getApplication().messageBus
      .connect(testRootDisposable)
      .subscribe(VirtualFileManager.VFS_CHANGES, createListener(project))
    project.putUserData(YAML_TEST_THROW_EXCEPTION, false)
  }
  
  protected fun doAddFileTest(filePathInTask: String, text: String = "", checksProducer: (Task) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage,
      createYamlConfigs = true
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/Task.kt")
        }
      }
    }

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find directory of `${task.name}` task")

    GeneratorUtils.createChildFile(project, taskDir, filePathInTask, text)
    checksProducer(task).forEach(FileCheck::check)
  }

  protected fun createCourseForCopyTests(): Course {
    fun LessonBuilder<*>.taskTemplate(name: String) {
      eduTask(name) {
        dir("src") {
          taskFile("Task1.kt")
          dir("foo") {
            taskFile("Task2.kt")
            taskFile("Task3.kt")
          }
          taskFile("bar/Task4.kt")
        }

        taskFile("additional_file1.txt")
        dir("foo") {
          taskFile("additional_file2.txt")
          taskFile("additional_file3.txt")
        }
        taskFile("bar/additional_file4.txt")

        dir("test") {
          taskFile("Tests1.kt")
          dir("foo") {
            taskFile("Tests2.kt")
            taskFile("Tests3.kt")
          }
          taskFile("bar/Tests4.kt")
        }
      }
    }

    return courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage,
      createYamlConfigs = true
    ) {

      lesson("lesson1") {
        taskTemplate("task1")
        taskTemplate("task2")
        taskTemplate("task3")
      }

      lesson("lesson2") {
        taskTemplate("task1")
        taskTemplate("task2")
      }

      section("section1") {
        lesson("lesson1") {
          taskTemplate("task1")
          taskTemplate("task2")
          taskTemplate("task3")
        }

        lesson("lesson2") {
          taskTemplate("task1")
          taskTemplate("task2")
        }

        lesson("lesson3") {
          taskTemplate("task1")
        }
      }

      section("section2") {
        lesson("lesson1") {
          taskTemplate("task1")
          taskTemplate("task2")
          taskTemplate("task3")
        }

        lesson("lesson2") {
          taskTemplate("task1")
          taskTemplate("task2")
        }
      }
    }
  }

  protected fun doCopyFileTest(filePathInCourse: String, newParentPath: String, copyName: String? = null, checksProducer: (Course) -> List<FileCheck>) {
    val course = createCourseForCopyTests()

    val requestor = VirtualFileListenerTestBase::class.java
    val fileToCopy = findFile(filePathInCourse)
    val newParentFolder = findFile(newParentPath)
    val newName = copyName ?: fileToCopy.name

    runWriteAction {
      copy(requestor, fileToCopy, newParentFolder, newName)
    }

    checkAllStudyItemsAreNotTaskWithType(course)

    checksProducer(course).forEach(FileCheck::check)
  }

  private fun checkAllStudyItemsAreNotTaskWithType(course: Course) {
    fun checkLesson(lesson: Lesson) {
      for (task in lesson.items)
        if (task is TaskWithType)
          error("There should be no instances of TaskWithType")
    }

    fun checkSection(section: Section) {
      for (lesson in section.items)
        checkLesson(lesson as Lesson)
    }

    for (courseItem in course.items) {
      if (courseItem is Section)
        checkSection(courseItem)
      else
        checkLesson(courseItem as Lesson)
    }
  }

  /**
   * This is a simple copy method, similar to [VfsUtil.copyFile], [VfsUtil.copy].
   * To copy files, these methods first create a new file and then fill its contents.
   * So no [VFileCopyEvent] is fired, instead, [VFileCreateEvent] and [VFileContentChangeEvent] are called.
   * This [copy] method calls the [VirtualFile.copy], so it is theoretically possible to have the [VFileCopyEvent].
   * Unfortunately, the file system used for tests is non-real, and it indirectly calls [VfsUtil.copyFile] to copy files.
   */
  protected fun copy(requestor: Any?, file: VirtualFile, newParent: VirtualFile, newName: String) {
    @Suppress("UnsafeVfsRecursion")
    if (file.isDirectory) {
      val copiedDirectory = newParent.createChildDirectory(requestor, newName)
      for (child in file.children) {
        copy(requestor, child, copiedDirectory, child.name)
      }
    } else
      file.copy(requestor, newParent, newName)
  }

  protected fun doRemoveFileTest(filePathInCourse: String, checksProducer: (Course) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/TaskFile.kt")
          taskFile("additionalFile.txt")
          taskFile("test/${FakeGradleConfigurator.TEST_FILE_NAME}")
          dir("src/packageName") {
            taskFile("TaskFile2.kt")
            taskFile("TaskFile3.kt")
          }
          dir("additional_files") {
            taskFile("additional_file2.txt")
            taskFile("additional_file3.txt")
          }
          taskFile("additional_files.txt") // some file with the name starting as the folder name
          dir("test/packageName") {
            taskFile("Tests2.kt")
            taskFile("Tests3.kt")
          }
        }
      }
      section("section1") {
        lesson("lesson2")
      }
    }

    val file = findFile(filePathInCourse)
    runWriteAction { file.delete(CCVirtualFileListenerTest::class.java) }
    checksProducer(course).forEach(FileCheck::check)
  }

  protected fun doRenameFileTest(filePathInCourse: String, newName: String, checksProducer: (Course) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          dir("src/packageName") {
            taskFile("Task1.kt")
            taskFile("Task2.kt")
          }

          dir("additional_files") {
            taskFile("additional_file1.txt")
            taskFile("additional_file2.txt")
          }
          dir("test/packageName") {
            taskFile("Test1.kt")
            taskFile("Test2.kt")
          }
        }
      }
    }

    val file = findFile(filePathInCourse)
    val psiFileSystemItem = if (file.isDirectory) {
      PsiManager.getInstance(project).findDirectory(file) ?: error("Can't find psi directory for $file")
    } else {
      PsiManager.getInstance(project).findFile(file) ?: error("Can't find psi file for $file")
    }
    myFixture.renameElement(psiFileSystemItem, newName)
    val checks = checksProducer(course)
    checks.forEach(FileCheck::check)

    val dialog = EduTestDialog()
    withEduTestDialog(dialog) {
      UndoManager.getInstance(project).undo(null)
    }
    checks.map(FileCheck::invert).forEach(FileCheck::check)
    withEduTestDialog(dialog) {
      UndoManager.getInstance(project).redo(null)
    }
    checks.forEach(FileCheck::check)
  }

  protected fun doMoveTest(filePath: String, newParentPath: String, checksProducer: (Course) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          dir("src") {
            taskFile("Task1.kt")
            dir("foo") {
              taskFile("Task2.kt")
              taskFile("Task3.kt")
            }
            taskFile("bar/Task4.kt")
          }

          taskFile("additional_file1.txt")
          dir("foo") {
            taskFile("additional_file2.txt")
            taskFile("additional_file3.txt")
          }
          taskFile("bar/additional_file4.txt")

          dir("test") {
            taskFile("Tests1.kt")
            dir("foo") {
              taskFile("Tests2.kt")
              taskFile("Tests3.kt")
            }
            taskFile("bar/Tests4.kt")
          }
        }
      }
    }

    val requestor = CCVirtualFileListenerTest::class.java

    runWriteAction {
      val dir = LightPlatformTestCase.getSourceRoot()
        .createChildDirectory(requestor, "non_course_dir")
      dir.createChildData(requestor, "non_course_file1.txt")
      dir.createChildData(requestor, "non_course_file2.txt")
    }

    val file = findFile(filePath)
    val newParent  = findFile(newParentPath)
    runWriteAction { file.move(requestor, newParent) }

    checksProducer(course).forEach(FileCheck::check)
  }
}
