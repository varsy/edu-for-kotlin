package com.jetbrains.edu.assistant.validation.test

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.inspection.InspectionProvider
import com.jetbrains.edu.learning.eduAssistant.inspection.getInspectionsWithIssues
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runners.Parameterized
import kotlin.test.assertNotNull

@Category(AiAutoQualityCodeTests::class)
class TaskBasedAssistantTest(lesson: String, task: String) : ExternalResourcesTest(lesson, task) {
  override val course: Course = createKotlinOnboardingMockCourse()

  private val language: Language = course.languageById ?: error("Language could not be determined")

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = createKotlinOnboardingMockCourse().lessons.flatMap { lesson ->
      lesson.taskList.filterIsInstance<EduTask>().map { task ->
        arrayOf(lesson.name, task.name)
      }
    }
  }

  private fun getInspections(language: Language): List<LocalInspectionTool> {
    return InspectionProvider.getInspections(language)
  }

  private fun getCodeIssues(code: String?, fileName: String, project: Project, language: Language, inspections: List<LocalInspectionTool>): List<String>? {
    return code?.let {
      PsiFileFactory.getInstance(project).createFileFromText(fileName, language, it)
    }?.getInspectionsWithIssues(inspections)?.map { issue -> issue.id }
  }

  @Test
  fun testGetHints() {
    val task = getTargetTask()
    val taskProcessor = TaskProcessorImpl(task)
    val userCode = task.taskFiles.values.firstNotNullOfOrNull { it.getText(project) }

    val response = getHint(taskProcessor, userCode)
    refreshProject()

    val inspections = getInspections(language)

    val issuesUser = getCodeIssues(userCode, "file", project, language, inspections)
    val issuesAi = getCodeIssues(response.codeHint?.value, "file2", project, language, inspections)

    assertNotNull(issuesAi, "Failed to obtain AI issues. The issue list should not be null.")
    assertNotNull(issuesUser, "Failed to obtain user issues. The issue list should not be null.")
    assert(issuesAi.size <= issuesUser.size) { "The number of issues generated by AI (${issuesAi.size}) should not exceed the number of user-generated issues (${issuesUser.size})." }
  }
}
