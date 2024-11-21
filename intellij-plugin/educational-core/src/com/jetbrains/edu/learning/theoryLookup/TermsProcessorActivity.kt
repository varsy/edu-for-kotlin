package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.isMarketplaceStudentCourse
import com.jetbrains.educational.ml.theory.lookup.term.Term

/**
 * Represents an activity that finds terms in a course.
 */
class TermsProcessorActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (project.isDisposed || isUnitTestMode) return
    if (!project.isMarketplaceStudentCourse()) return
    val course = project.course ?: return

    val termsManager = TheoryLookupTermsManager.getInstance(project)

    with(termsManager) {
      if (!areCourseTermsLoaded()) return
      val courseTerms = getCourseTerms(project, course)
      setTheoryLookupProperties(courseTerms.toTheoryLookupProperties())
    }
  }

  private fun Map<Task, List<Term>>.toTheoryLookupProperties(): TheoryLookupProperties {
    return TheoryLookupProperties(mapKeys { it.key.id })
  }

  private suspend fun getCourseTerms(project: Project, course: Course): Map<Task, List<Term>> {
    // TODO(make a call to a connector instead of following commented code)
    return emptyMap()
    /*
    course.allTasks.forEach { task ->
      launch {
        termsManager.extractTerms(task)
      }
    }
    val text = runReadAction {
      task.getDescriptionFile(project)?.getTextFromTaskTextFile()
    } ?: return
    val termsProvider = TermsProvider()
    val termsList = termsProvider.findTermsAndDefinitions(text).getOrThrow()
    */
  }
}