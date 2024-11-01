package com.jetbrains.edu.aiHints.core.ui

import com.intellij.ui.InlineBanner
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.ui.EduColors
import org.jetbrains.annotations.Nls
import javax.swing.BorderFactory
import javax.swing.border.CompoundBorder

class HintInlineBanner(@Nls message: String) : InlineBanner(message) {
  init {
    setIcon(EduAiHintsIcons.Hint)
    isOpaque = false
    border = createBorder()
    background = EduColors.aiGetHintInlineBannersBackgroundColor
    toolTipText = EduAIHintsCoreBundle.message("hints.label.ai.generated.content.tooltip")
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