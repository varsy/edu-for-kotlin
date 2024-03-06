package com.jetbrains.edu.learning.actions

import com.intellij.diff.chains.DiffRequestChain
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.util.ui.JButtonAction
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.CancelHintAction.Companion.closeDiffWindow
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import javax.swing.JButton

// TODO: possibly reuse ApplyCodeAction class
class AcceptHintAction : JButtonAction(EduCoreBundle.message("action.Educational.Assistant.AcceptHint.button")) {
  override fun getActionUpdateThread() = ActionUpdateThread.EDT

  override fun createButton(): JButton =
    object : JButton(templatePresentation.text) {
      override fun isDefaultButton(): Boolean = true
      override fun isEnabled(): Boolean = true
      override fun isFocusable(): Boolean = true
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isNextStepHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return

    val diffRequestChain = e.getDiffRequestChain() ?: return
    val fileNames = diffRequestChain.getUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST).takeIf { !it.isNullOrEmpty() } ?: return

    if (tryApplyTexts(project, diffRequestChain, fileNames, ACTION_ID, this.templatePresentation.text)) {
      diffRequestChain.putUserData(NextStepHintAction.IS_ACCEPTED_HINT, true)
      project.closeDiffWindow(e)
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Assistant.AcceptHint"

    fun AnActionEvent.isNextStepHintDiff() = getDiffRequestChain()?.getUserData(NextStepHintAction.NEXT_STEP_HINT_DIFF_FLAG) == true

    fun AnActionEvent.getDiffRequestChain(): DiffRequestChain? {
      val chainDiffVirtualFile = getData(CommonDataKeys.VIRTUAL_FILE) as? ChainDiffVirtualFile
      return chainDiffVirtualFile?.chain
    }
  }
}
