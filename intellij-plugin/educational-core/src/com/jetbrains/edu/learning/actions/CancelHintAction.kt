package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.ApplyCodeActionBase.Companion.getDiffRequestChain
import com.jetbrains.edu.learning.actions.ApplyCodeActionBase.Companion.isNextStepHintDiff
import com.jetbrains.edu.learning.actions.EduActionUtils.closeLastActiveFileEditor
import com.jetbrains.edu.learning.invokeLater
import org.jetbrains.annotations.NonNls
import javax.swing.JButton

class CancelHintAction : DumbAwareAction(), CustomComponentAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isNextStepHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val diffRequestChain = e.getDiffRequestChain() ?: return
    diffRequestChain.putUserData(NextStepHintAction.IS_ACCEPTED_HINT, false)

    val project = e.project ?: return
    project.invokeLater {
      project.closeLastActiveFileEditor(e)
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun createCustomComponent(presentation: Presentation, place: String) =
    object : JButton(presentation.text) {
      override fun isDefaultButton(): Boolean = false
      override fun isFocusable(): Boolean = true
    }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Assistant.CancelHint"
  }
}
