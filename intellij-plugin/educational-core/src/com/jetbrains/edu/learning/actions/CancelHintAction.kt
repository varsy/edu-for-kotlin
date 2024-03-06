package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JButtonAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.AcceptHintAction.Companion.getDiffRequestChain
import com.jetbrains.edu.learning.actions.AcceptHintAction.Companion.isNextStepHintDiff
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import javax.swing.JButton

class CancelHintAction : JButtonAction(EduCoreBundle.message("action.Educational.Assistant.CancelHint.button")) {

  override fun getActionUpdateThread() = ActionUpdateThread.EDT
  override fun createButton(): JButton =
    object : JButton(templatePresentation.text) {
      override fun isDefaultButton(): Boolean = false
      override fun isEnabled(): Boolean = true
      override fun isFocusable(): Boolean = true
    }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isNextStepHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val diffRequestChain = e.getDiffRequestChain() ?: return
    diffRequestChain.putUserData(NextStepHintAction.IS_ACCEPTED_HINT, false)

    val project = e.project ?: return
    project.closeDiffWindow(e)
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Assistant.CancelHint"

    fun Project.closeDiffWindow(e: AnActionEvent) {
      val fileEditorManager = FileEditorManager.getInstance(this)
      val fileEditor = e.getData(PlatformDataKeys.LAST_ACTIVE_FILE_EDITOR) ?: return
      fileEditorManager.closeFile(fileEditor.file)
    }
  }
}
