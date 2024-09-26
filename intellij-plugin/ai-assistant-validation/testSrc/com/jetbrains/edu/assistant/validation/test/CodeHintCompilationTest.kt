package com.jetbrains.edu.assistant.validation.test

import com.jetbrains.edu.assistant.validation.util.StudentSolutionRecord
import com.jetbrains.edu.assistant.validation.util.downloadSolution
import com.jetbrains.edu.assistant.validation.util.parseCsvFile
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import junit.framework.TestCase
import org.junit.Ignore
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runners.Parameterized
import kotlin.io.path.Path

@Category(AiAutoQualityCodeTests::class)
@Ignore
class CodeHintCompilationTest(private val lessonName: String, private val taskName: String) : ExternalResourcesTest(lessonName, taskName) {

  override val course: Course = createKotlinOnboardingMockCourse()
  private val maxSolutions by lazy { System.getProperty("max.solutions.testing").toIntOrNull() ?: 1 }

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = createKotlinOnboardingMockCourse().lessons.flatMap { lesson ->
      lesson.taskList.filterIsInstance<EduTask>().map { task ->
        arrayOf(lesson.name, task.name)
      }
    }

    private val studentSolutions = parseCsvFile(Path("../../solutionsForValidation/tt_data_for_tests_version1.csv")) { record ->
      StudentSolutionRecord.buildFrom(record)
    } ?: error("Student solutions were not found")
  }

  @Test
  fun testCodeHintCompilation() {
    val task = getTargetTask()
    val taskProcessor = TaskProcessorImpl(task)
    studentSolutions.filter { it.lessonName == lessonName && it.taskName == taskName }.shuffled().take(maxSolutions)
      .map { it.code }.firstOrNull()?.let {
      downloadSolution(task, project, it)
      getHint(taskProcessor)
      refreshProject()
      runCheck(task) { checkerResult ->
        TestCase.assertNotSame("Compilation error in the code: $it", checkerResult.message, CheckUtils.COMPILATION_FAILED_MESSAGE)
      }
    }
  }
}
