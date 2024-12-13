package com.jetbrains.edu.learning.taskToolWindow.ui.check

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.actions.ActionWithProgressIcon
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.ui.isDefault
import org.apache.commons.lang3.StringUtils
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.event.ActionEvent
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Panel with button inside and progress icon if action is appropriate
 */
class CheckPanelButtonComponent private constructor() : JPanel(BorderLayout()) {
  /**
   * @param[action] action to execute when button is clicked. Panel will also have process icon when action is being executed.
   * @param[isDefault] parameter specifies whether button is painted as default or not. `false` by default.
   * @param[isEnabled] parameter for enabling/disabling button. `true` by default.
   * @param[isSuccessOfPromptActions] parameter to show a tool tip about fixing prompt actions. `true` by default.
   *
   * @see com.jetbrains.edu.learning.actions.ActionWithProgressIcon
   */
  constructor(action: ActionWithProgressIcon, isDefault: Boolean = false, isEnabled: Boolean = true, isSuccessOfPromptActions: Boolean = true) : this() {
    val buttonPanel = createButtonPanel(action = action, isDefault = isDefault, isSuccessOfPromptActions = isSuccessOfPromptActions, isEnabled = isEnabled)
    add(buttonPanel, BorderLayout.WEST)

    val spinnerPanel = action.spinnerPanel
    if (spinnerPanel != null) {
      add(spinnerPanel, BorderLayout.CENTER)
    }
  }

  /**
   * @param[action] action to execute when button is clicked.
   * @param[isDefault] parameter specifies whether button is painted as default or not. `false` by default.
   * @param[isEnabled] parameter for enabling/disabling button. `true` by default.
   */
  constructor(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    customButtonText: String? = null
  ) : this() {
    val buttonPanel =
      createButtonPanel(action, isDefault = isDefault, isEnabled = isEnabled, customButtonText = customButtonText)
    add(buttonPanel)
  }

  private fun createButtonPanel(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    isSuccessOfPromptActions: Boolean = true,
    customButtonText: String? = null
  ): JPanel {
    val button = createButton(action, isDefault = isDefault, isEnabled = isEnabled, isSuccessOfPromptActions = isSuccessOfPromptActions, customButtonText = customButtonText)
    return createButtonPanel(button)
  }

  private fun createButton(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    isSuccessOfPromptActions: Boolean = true,
    customButtonText: String? = null
  ): JButton {
    val text = if (customButtonText != null) StringUtils.abbreviate(customButtonText, 25) else action.templatePresentation.text
    val button = JButton(text).apply {
      this.isEnabled = isEnabled
      this.isFocusable = isEnabled
      this.isDefault = isDefault
      if (!isSuccessOfPromptActions) toolTipText = EduCoreBundle.message("cognifire.ui.tool.tip.text.fix.todo")
    }
    if (isEnabled) {
      button.addActionListener { e ->
        performAnAction(e, this, action)
      }
    }
    return button
  }

  private fun createButtonPanel(button: JComponent): JPanel {
    val buttonPanel = JPanel(GridLayout(1, 1, 5, 0))
    buttonPanel.add(button)
    val gridBagPanel = JPanel(GridBagLayout())
    val buttonPanelGridBagConstraints = GridBagConstraints(
      0, 0, 1, 1, 1.0, 0.0,
      GridBagConstraints.CENTER,
      GridBagConstraints.NONE,
      JBUI.insetsTop(8), 0, 0
    )
    gridBagPanel.add(buttonPanel, buttonPanelGridBagConstraints)
    return gridBagPanel
  }
}

private fun performAnAction(actionEvent: ActionEvent, component: JComponent, action: AnAction) {
  val dataContext = DataManager.getInstance().getDataContext(component)
  val event = AnActionEvent(
    null,
    dataContext,
    CheckPanel.ACTION_PLACE,
    PresentationFactory().getPresentation(action),
    ActionManager.getInstance(),
    actionEvent.modifiers
  )

  ActionUtil.performActionDumbAwareWithCallbacks(action, event)
}