package com.jetbrains.edu.ai.translation.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.ui.AITranslationNotificationManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered")
class ResetCourseTranslation : AITranslationActionBase() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    AITranslationNotificationManager.closeExistingNotifications(project)
    TranslationLoader.getInstance(project).resetCourseTranslation(course)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return
    e.presentation.isEnabledAndVisible = !TranslationLoader.getInstance(project).isRunning
  }

  companion object {
    @Suppress("unused")
    @NonNls
    private const val ACTION_ID: String = "Educational.ResetCourseTranslation"
  }
}