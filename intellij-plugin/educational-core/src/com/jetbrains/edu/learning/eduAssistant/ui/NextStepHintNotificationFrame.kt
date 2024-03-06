package com.jetbrains.edu.learning.eduAssistant.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.ui.IdeUiService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.JBColor
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.util.maximumWidth
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.EducationalCoreIcons
import java.awt.BorderLayout
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.text.DefaultCaret

class NextStepHintNotificationFrame(message: String, action: AnAction?, actionTargetParent: JPanel?, closePanel: () -> Unit) : JFrame() {
  init {
    contentPane.background = BACKGROUND_COLOR
    val contentPanel = JPanel(BorderLayout())
    contentPanel.isOpaque = false
    contentPanel.border = RoundedLineBorder(BORDER_COLOR, NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get())
    contentPanel.border = BorderFactory.createCompoundBorder(contentPanel.border, BorderFactory.createEmptyBorder(PADDING_BORDER, PADDING_BORDER, PADDING_BORDER, PADDING_BORDER))

    val iconPanel = JPanel(BorderLayout())
    iconPanel.isOpaque = false
    iconPanel.add(JBLabel(EducationalCoreIcons.aiAssistant), BorderLayout.NORTH)
    contentPanel.add(iconPanel, BorderLayout.WEST)

    val cancelButton = JBLabel(AllIcons.Windows.CloseActive)
    cancelButton.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        closePanel()
      }
    })
    val cancelPanel = JPanel(BorderLayout())
    cancelPanel.isOpaque = false
    cancelPanel.add(cancelButton, BorderLayout.NORTH)
    contentPanel.add(cancelPanel, BorderLayout.EAST)

    val centerPanel = JPanel(VerticalLayout(JBUI.scale(PADDING_BORDER)))
    centerPanel.isOpaque = false
    centerPanel.border = BorderFactory.createEmptyBorder(0, PADDING_BORDER, 0, PADDING_BORDER)
    centerPanel.background = null

    val editorPane = JEditorPane()
    editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
    val caret = editorPane.caret as DefaultCaret
    caret.updatePolicy = DefaultCaret.NEVER_UPDATE
    editorPane.isEditable = false
    editorPane.contentType = "text/html"
    editorPane.text = message
    editorPane.background = null
    editorPane.isOpaque = false
    editorPane.maximumWidth = (actionTargetParent?.width ?: editorPane.width) - MARGIN
    centerPanel.add(editorPane)

    action?.let {
      centerPanel.add(createAction(action))
    }

    contentPanel.add(centerPanel, BorderLayout.CENTER)
    add(contentPanel)
    pack()
  }

  private fun createAction(action: AnAction): JComponent =
    object : LinkLabel<AnAction>(action.templateText, action.templatePresentation.icon, { lnk, act ->
      val dataContext = DataManager.getInstance().getDataContext(lnk)
      val event = AnActionEvent.createFromAnAction(act, null as InputEvent?, "Notification", dataContext)
      IdeUiService.getInstance().performActionDumbAwareWithCallbacks(act, event)
                                                                                        }, action) {

      override fun getTextColor() = JBUI.CurrentTheme.Link.Foreground.ENABLED
    }

  companion object {
    const val PADDING_BORDER: Int = 10
    const val MARGIN: Int = 40
    val BORDER_COLOR = JBColor(0xDCCBFB, 0x8150BE)
    val BACKGROUND_COLOR = JBColor(0xFAF5FF, 0x2F2936)
  }
}