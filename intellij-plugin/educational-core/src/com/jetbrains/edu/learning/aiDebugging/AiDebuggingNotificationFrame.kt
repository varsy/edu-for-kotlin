package com.jetbrains.edu.learning.aiDebugging

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.JBColor
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.AnActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.util.maximumHeight
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil.HTML_MIME
import com.jetbrains.edu.EducationalCoreIcons
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.text.DefaultCaret

class AiDebuggingNotificationFrame(@Nls message: String, action: AnAction?, actionTargetParent: JPanel?, close: () -> Unit) : JFrame() {

  init {
    contentPane.background = BACKGROUND_COLOR

    val centerPanel = createCenterPanel().apply {
      add(createJEditorPane(actionTargetParent, message))
      action?.let { add(createActionLink(it)) }
    }

    val contentPanel = createContentPanel().apply {
      add(createIconPanel(), BorderLayout.WEST)
      add(createCancelPanel(close), BorderLayout.EAST)
      add(centerPanel, BorderLayout.CENTER)
    }

    add(contentPanel)
    pack()
  }

  private fun createCenterPanel(): JPanel = JPanel(VerticalLayout(JBUI.scale(PADDING_BORDER))).apply {
    isOpaque = false
    border = BorderFactory.createEmptyBorder(0, PADDING_BORDER, 0, PADDING_BORDER)
    background = null
  }

  private fun createContentPanel(): JPanel = JPanel(BorderLayout()).apply {
    isOpaque = false
    border = BorderFactory.createCompoundBorder(
      RoundedLineBorder(BORDER_COLOR, NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get()),
      BorderFactory.createEmptyBorder(PADDING_BORDER, PADDING_BORDER, PADDING_BORDER, PADDING_BORDER)
    )
  }

  private fun createJEditorPane(actionTargetParent: JPanel?, @Nls message: String): JEditorPane = JEditorPane().apply {
    putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    isOpaque = false
    isEditable = false
    (caret as DefaultCaret).updatePolicy = DefaultCaret.NEVER_UPDATE
    contentType = HTML_MIME
    text = message
    background = null
    maximumSize = JBUI.size((actionTargetParent?.width ?: width) - MARGIN, maximumHeight)
  }

  private fun createActionLink(action: AnAction): JComponent = AnActionLink(action.templateText, action).apply {
    icon = action.templatePresentation.icon
    foreground = JBUI.CurrentTheme.Link.Foreground.ENABLED
  }

  private fun createIconPanel(): JPanel = JPanel(BorderLayout()).apply {
    isOpaque = false
    add(JBLabel(EducationalCoreIcons.aiAssistant), BorderLayout.NORTH)
  }

  private fun createCancelPanel(close: () -> Unit): JPanel = JPanel(BorderLayout()).apply {
    val cancelButton = JBLabel(AllIcons.Windows.CloseActive)
    cancelButton.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        close()
      }
    })
    isOpaque = false
    add(cancelButton, BorderLayout.NORTH)
  }

  companion object {
    private val BORDER_COLOR: JBColor = JBColor(0xDCCBFB, 0x8150BE)

    private val BACKGROUND_COLOR: JBColor = JBColor(0xFAF5FF, 0x2F2936)

    private const val PADDING_BORDER: Int = 10

    private const val MARGIN: Int = 40
  }
}
