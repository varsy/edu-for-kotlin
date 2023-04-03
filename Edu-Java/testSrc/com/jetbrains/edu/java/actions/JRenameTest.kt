package com.jetbrains.edu.java.actions

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiJavaFile
import com.intellij.refactoring.rename.PsiElementRenameHandler
import com.intellij.refactoring.util.CommonRefactoringUtil
import com.intellij.testFramework.MapDataContext
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.testAction

class JRenameTest : EduActionTestCase() {

  fun `test forbid public class renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          javaTaskFile("Task1.java", """
              public class Task1 {}
          """)
        }
      }
    }
    myFixture.openFileInEditor(findFile("lesson1/task1/Task1.java"))
    val javaFile = myFixture.file as PsiJavaFile
    doRenameAction(course, javaFile.findClass("Task1"), "Task2", shouldShowErrorDialog = true)
    val task = course.findTask("lesson1", "task1")
    assertNotNull(task.getTaskFile("Task1.java"))
    assertNull(task.getTaskFile("Task2.java"))
    myFixture.checkResult("""
        public class Task1 {}
    """.trimIndent())
  }

  fun `test rename inner class in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          javaTaskFile("Task1.java", """
              public class Task1 {}
              class Foo {}
          """)
        }
      }
    }
    myFixture.openFileInEditor(findFile("lesson1/task1/Task1.java"))
    val javaFile = myFixture.file as PsiJavaFile
    doRenameAction(course, javaFile.findClass("Foo"), "Bar", shouldShowErrorDialog = false)
    myFixture.checkResult("""
        public class Task1 {}
        class Bar {}
    """.trimIndent())
  }

  fun `test rename student created public class in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          javaTaskFile("Task1.java", """
              public class Task1 {}
          """)
        }
      }
    }
    val taskDirectory = findFile("lesson1/task1")
    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, taskDirectory, "Foo.java", """
        public class Foo {}
      """.trimIndent())
    }

    myFixture.openFileInEditor(findFile("lesson1/task1/Foo.java"))
    val javaFile = myFixture.file as PsiJavaFile
    doRenameAction(course, javaFile.findClass("Foo"), "Bar", shouldShowErrorDialog = false)

    val task = course.findTask("lesson1", "task1")
    assertNotNull(task.getTaskFile("Bar.java"))
    assertNull(task.getTaskFile("Foo.java"))
    myFixture.checkResult("""
      public class Bar {}
    """.trimIndent())
  }

  fun `test rename public class in CC mode`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          javaTaskFile("Task1.java", """
              public class Task1 {}
          """)
        }
      }
    }
    myFixture.openFileInEditor(findFile("lesson1/task1/Task1.java"))
    val javaFile = myFixture.file as PsiJavaFile
    doRenameAction(course, javaFile.findClass("Task1"), "Task2", shouldShowErrorDialog = false)
    val task = course.findTask("lesson1", "task1")
    assertNotNull(task.getTaskFile("Task2.java"))
    assertNull(task.getTaskFile("Task1.java"))
    myFixture.checkResult("""
        public class Task2 {}
    """.trimIndent())
  }

  private fun PsiJavaFile.findClass(name: String): PsiClass = classes.find { it.name == name }!!

  private fun doRenameAction(
    course: Course,
    target: Any,
    newName: String,
    shouldShowErrorDialog: Boolean
  ) {
    val dataContext = when (target) {
      is String -> dataContext(findFile(target))
      is VirtualFile -> dataContext(target)
      is PsiElement -> dataContext(target)
      else -> error("Unexpected class of target: ${target.javaClass.name}. Only String, VirtualFile or PsiElement are supported")
    }

    var isErrorDialogShown = false
    try {
      withVirtualFileListener(course) {
        testAction(IdeActions.ACTION_RENAME, dataContext.withRenameDefaultName(newName))
      }
    }
    catch (e: CommonRefactoringUtil.RefactoringErrorHintException) {
      if (e.message == EduCoreBundle.message("error.invalid.rename.message")) {
        isErrorDialogShown = true
      }
      else {
        throw e
      }
    }

    assertTrue("Error dialog is ${if (!isErrorDialogShown) "" else "not "}shown", isErrorDialogShown == shouldShowErrorDialog)
  }

  private fun MapDataContext.withRenameDefaultName(newName: String): MapDataContext =
    apply { put(PsiElementRenameHandler.DEFAULT_NAME, newName) }
}
