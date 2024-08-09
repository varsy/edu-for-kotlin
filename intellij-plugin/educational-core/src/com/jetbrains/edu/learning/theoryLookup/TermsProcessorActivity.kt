package com.jetbrains.edu.learning.theoryLookup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/**
 * Represents an activity that finds terms in a course.
 */
class TermsProcessorActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (project.isDisposed || isUnitTestMode) return
    val course = StudyTaskManager.getInstance(project).course as? EduCourse ?: return

    val exceptionHandler = CoroutineExceptionHandler { _, exception ->
      EduNotificationManager.showErrorNotification(
        project,
        EduCoreBundle.message("theory.lookup.notification.failed.to.extract.terms.title"),
        exception.message ?:
        EduCoreBundle.message("theory.lookup.notification.failed.to.extract.terms.text")
      )
      thisLogger().error("Terms extracting failed", exception)
    }

    val termsManager = TermsManager.getInstance(project)
    supervisorScope {
      course.allTasks.forEach { task ->
        launch(Dispatchers.IO + exceptionHandler) { termsManager.extractTerms(task) }
      }
    }
  }
}
