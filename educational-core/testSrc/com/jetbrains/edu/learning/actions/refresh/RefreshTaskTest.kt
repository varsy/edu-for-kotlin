package com.jetbrains.edu.learning.actions.refresh

import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.RevertTaskAction
import com.jetbrains.edu.learning.yaml.YamlDeepLoader
import java.io.IOException

class RefreshTaskTest : EduTestCase() {

  fun testRefreshTask() {
    configureByTaskFile(1, 1, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(13)
    myFixture.type("test")
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    assertEquals("Look! There is placeholder.", myFixture.getDocument(myFixture.file).text)
  }

  fun testCaretOutside() {
    configureByTaskFile(1, 2, "taskFile2.txt")
    myFixture.editor.caretModel.moveToOffset(4)
    myFixture.type("test")
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }
    assertEquals("Look! There is placeholder.", myFixture.getDocument(myFixture.file).text)
  }

  fun testMultipleFiles() {
    configureByTaskFile(1, 3, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(5)
    myFixture.type("test")
    configureByTaskFile(1, 3, "taskFile2.txt")
    myFixture.editor.caretModel.moveToOffset(5)
    myFixture.type("test")
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }
    val fileName = "lesson1/task3/taskFile1.txt"
    val fileName2 = "lesson1/task3/taskFile2.txt"
    val file1 = myFixture.findFileInTempDir(fileName)
    val file2 = myFixture.findFileInTempDir(fileName2)
    myFixture.openFileInEditor(file1)
    assertEquals("Look! There is my placeholder.", myFixture.editor.document.text)
    myFixture.openFileInEditor(file2)
    assertEquals("Look! There is placeholder.", myFixture.editor.document.text)
  }

  fun `test invisible files`() {
    // Emulate load course from yaml after project reopening
    StudyTaskManager.getInstance(project).course = YamlDeepLoader.loadCourse(project)

    configureByTaskFile(1, 4, "taskFile1.txt")
    myFixture.editor.caretModel.moveToOffset(0)
    myFixture.type("test")
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    withEduTestDialog(EduTestDialog(Messages.OK)) {
      testAction(RevertTaskAction.ACTION_ID)
    }

    val taskDir = findFile("lesson1/task4")

    fileTree {
      file("taskFile1.txt", "TaskFile1")
      file("taskFile2.txt", "TaskFile2")
      file("task.md")
      file("task-info.yaml")
    }.assertEquals(taskDir, myFixture)
  }

  @Throws(IOException::class)
  override fun createCourse() {
    StudyTaskManager.getInstance(myFixture.project).course = courseWithFiles(createYamlConfigs = true) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt", """
            Look! There <p>is</p> placeholder.
          """)
        }
        eduTask {
          taskFile("taskFile2.txt", """
            Look! There <p>is</p> placeholder.
          """)
        }
        eduTask {
          taskFile("taskFile1.txt", """
            Look! There <p>is</p> my placeholder.
          """)
          taskFile("taskFile2.txt", """
            Look! There <p>is</p> placeholder.
          """)
        }
        eduTask {
          taskFile("taskFile1.txt", "TaskFile1")
          taskFile("taskFile2.txt", "TaskFile2", visible = false)
        }
      }
    }
  }

}
