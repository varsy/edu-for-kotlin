package com.jetbrains.edu.aiHints.python

import com.intellij.testFramework.utils.vfs.getPsiFile
import com.jetbrains.edu.aiHints.core.StringExtractor
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask
import com.jetbrains.python.PythonLanguage
import org.junit.Test

class PyStringExtractorTest : EduTestCase() {
  override fun createCourse() {
    courseWithFiles {
      lesson("python_lesson") {
        eduTask("python_task") {
          taskFile(
            "task.py", text = """
              a = "referenced"
              def foo():
                print("double")
                print('single')
                print(f"formatted {1 + 3}")
                print(a)
                
              def single_fun():
                return 'single_fun'
                
              def double_fun():
                return "double_fun"
                
              def formatted_fun():
                return f"formatted {a}"
            """.trimIndent()
          )
        }
      }
    }
  }

  @Test
  fun `test getting map of function signatures to strings`() {
    val psiFile = StudyTaskManager.getInstance(project).course
                    ?.findTask("python_lesson", "python_task")
                    ?.getTaskFile("task.py")
                    ?.getVirtualFile(project)
                    ?.getPsiFile(project) ?: error("Failed to extract PsiFile")

    val expectedResult = mapOf(
      FunctionSignature(
        "foo",
        listOf(),
        null,
        null,
        null
      ) to listOf("referenced", "double", "single", "formatted {1 + 3}"),
      FunctionSignature(
        "single_fun",
        listOf(),
        null,
        null,
        null
      ) to listOf("single_fun"),
      FunctionSignature(
        "double_fun",
        listOf(),
        null,
        null,
        null
      ) to listOf("double_fun"),
      FunctionSignature(
        "formatted_fun",
        listOf(),
        null,
        null,
        null
      ) to listOf("referenced", "formatted {a}")
    ).let(::FunctionsToStrings)

    assertEquals(expectedResult, StringExtractor.getFunctionsToStringsMap(psiFile, PythonLanguage.INSTANCE))
  }
}