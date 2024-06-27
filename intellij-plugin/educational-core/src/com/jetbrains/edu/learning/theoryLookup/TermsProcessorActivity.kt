package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.isUnitTestMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

/**
 * Represents an activity that finds terms in a course.
 */
class TermsProcessorActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (project.isDisposed || isUnitTestMode) return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return

    val termsManager = TermsManager.getInstance(project)
    runBlockingCancellable {
      withContext(Dispatchers.IO) {
        supervisorScope {
          course.allTasks.forEach { task ->
            launch { termsManager.extractTerms(task) }
          }
        }
      }
    }
  }
}
