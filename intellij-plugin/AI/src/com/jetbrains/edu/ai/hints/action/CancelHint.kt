package com.jetbrains.edu.ai.hints.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.ApplyCodeActionBase.Companion.isNextStepHintDiff
import com.jetbrains.edu.learning.actions.EduActionUtils.closeLastActiveFileEditor
import com.jetbrains.edu.learning.actions.EduActionUtils.performAction
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.ui.isDefault
import org.jetbrains.annotations.NonNls
import javax.swing.JButton
import javax.swing.JComponent

class CancelHint : DumbAwareAction(), CustomComponentAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isNextStepHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    with(project) {
      invokeLater {
        closeLastActiveFileEditor(e)
      }
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent = JButton(presentation.text).apply {
    isFocusable = true
    isDefault = false
    addActionListener {
      performAction(this@CancelHint, this, place, presentation)
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Hints.CancelHint"
  }
}