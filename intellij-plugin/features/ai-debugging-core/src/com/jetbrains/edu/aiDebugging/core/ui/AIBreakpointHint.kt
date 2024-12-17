package com.jetbrains.edu.aiDebugging.core.ui

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.PositionTracker
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.ui.EduColors

class AIBreakpointHint(text: String, editor: Editor, offset: Int) {

  private var balloon: Balloon? = null

  init {
    runInEdt {
      balloon = createBalloon(text)

      val tracker = object : PositionTracker<Balloon>(editor.contentComponent) {
        override fun recalculateLocation(balloon: Balloon) =
          RelativePoint(editor.contentComponent, editor.visualPositionToXY(editor.offsetToVisualPosition(offset)))
      }

      balloon?.show(tracker, Balloon.Position.above)
    }
  }

  private fun createBalloon(text: String): Balloon =
    JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        getInternalTemplateText(BALLOON_TEXT_TEMPLATE_NAME, createTemplateArgs(text)),
        AIDebuggingIcons.AIHint,
        EduColors.aiGetHintInlineBannersBackgroundColor,
        null
      )
      .setBorderColor(EduColors.aiGetHintInlineBannersBorderColor)
      .setRequestFocus(false)
      .setCloseButtonEnabled(true)
      .setHideOnClickOutside(false)
      .createBalloon()

  fun close() {
    balloon?.hide()
  }

  private fun createTemplateArgs(text: String) = mapOf("text" to text)

  companion object {
    private const val BALLOON_TEXT_TEMPLATE_NAME: String = "text-balloon.html"
  }
}
