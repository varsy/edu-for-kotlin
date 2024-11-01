package com.jetbrains.edu.ai.hints.ui

import com.intellij.ui.InlineBanner
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.ui.EducationalAIIcons
import com.jetbrains.edu.learning.ui.EduColors
import org.jetbrains.annotations.Nls
import javax.swing.BorderFactory
import javax.swing.border.CompoundBorder

class HintInlineBanner(message: @Nls String) : InlineBanner(message) {
  init {
    setIcon(EducationalAIIcons.Hint)
    isOpaque = false
    border = createBorder()
    background = EduColors.aiGetHintInlineBannersBackgroundColor
    toolTipText = EduAIBundle.message("hints.label.ai.generated.content.tooltip")
  }

  private fun createBorder(): CompoundBorder = BorderFactory.createCompoundBorder(
    RoundedLineBorder(
      EduColors.aiGetHintInlineBannersBorderColor, NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get()
    ), JBUI.Borders.empty(BORDER_OFFSET)
  )

  companion object {
    private const val BORDER_OFFSET: Int = 10
  }
}