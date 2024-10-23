package com.jetbrains.edu.ai.hints.ui

import com.intellij.ui.InlineBanner
import com.intellij.ui.JBColor
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.ui.EducationalAIIcons
import org.jetbrains.annotations.Nls
import javax.swing.BorderFactory
import javax.swing.border.CompoundBorder

class HintInlineBanner(message: @Nls String) : InlineBanner(message) {
  init {
    setIcon(EducationalAIIcons.Hint)
    isOpaque = false
    border = createBorder()
    background = JBColor(BACKGROUND_COLOR_RGB, BACKGROUND_COLOR_DARK_RGB)
    toolTipText = EduAIBundle.message("hints.label.ai.generated.content.tooltip")
  }

  private fun createBorder(): CompoundBorder = BorderFactory.createCompoundBorder(
    RoundedLineBorder(
      JBColor(BORDER_COLOR_RGB, BORDER_COLOR_DARK_RGB), NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get()
    ), JBUI.Borders.empty(BORDER_OFFSET)
  )

  companion object {
    private const val BACKGROUND_COLOR_RGB: Int = 0xFAF5FF
    private const val BACKGROUND_COLOR_DARK_RGB: Int = 0x2F2936
    private const val BORDER_COLOR_RGB: Int = 0xDCCBFB
    private const val BORDER_COLOR_DARK_RGB: Int = 0x8150BE
    private const val BORDER_OFFSET: Int = 10
  }
}