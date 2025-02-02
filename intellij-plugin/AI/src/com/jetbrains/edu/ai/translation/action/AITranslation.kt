package com.jetbrains.edu.ai.translation.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.GotItTooltip
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.connector.TranslationServiceConnector
import com.jetbrains.edu.ai.translation.ui.AITranslationNotificationManager
import com.jetbrains.edu.ai.translation.ui.CourseTranslationPopup
import com.jetbrains.edu.ai.ui.EducationalAIIcons
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse

@Suppress("ComponentNotRegistered")
class AITranslation : AITranslationActionBase() {
  private val tooltip: GotItTooltip = createTooltip()

  init {
    templatePresentation.icon = EducationalAIIcons.Translation
    templatePresentation.hoveredIcon = EducationalAIIcons.TranslationHovered
    templatePresentation.selectedIcon = EducationalAIIcons.TranslationPressed

    tooltip.assignTo(templatePresentation, GotItTooltip.BOTTOM_MIDDLE)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    AITranslationNotificationManager.closeExistingNotifications(project)

    val popup = CourseTranslationPopup(project, course)
    val relativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(this, e)
    popup.show(relativePoint)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    val course = project.course as? EduCourse ?: return
    if (isActionUnavailable(project, course)) return

    e.presentation.icon = if (TranslationProjectSettings.isCourseTranslated(project)) {
      EducationalAIIcons.TranslationEnabled
    }
    else {
      EducationalAIIcons.Translation
    }

    e.presentation.isEnabledAndVisible = !TranslationLoader.getInstance(project).isRunning
  }

  private fun createTooltip(): GotItTooltip {
    val translationServiceConnector = TranslationServiceConnector.getInstance()
    return GotItTooltip(TOOLTIP_ID, EduAIBundle.message("ai.translation.tooltip.text"), translationServiceConnector)
      .withHeader(EduAIBundle.message("ai.translation.tooltip.header"))
      .withPosition(Balloon.Position.below)
  }

  companion object {
    @Suppress("unused")
    const val ACTION_ID: String = "Educational.AITranslation"

    private const val TOOLTIP_ID: String = "ai.translation.tooltip"
  }
}
