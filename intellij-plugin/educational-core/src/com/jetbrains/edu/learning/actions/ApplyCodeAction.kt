package com.jetbrains.edu.learning.actions

import com.intellij.diff.chains.DiffRequestChain
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.PlatformDataKeys.LAST_ACTIVE_FILE_EDITOR
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.ui.GotItTooltip
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.AcceptHintAction.Companion.isNextStepHintDiff
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import org.jetbrains.annotations.NonNls
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent

/**
 * Allows users to replace the code in their editor with the one they're comparing it to in the Diff.
 * Not enabled in any Diff window by default.
 * Put a list of paths to VirtualFiles
 * that is used in the [com.intellij.diff.chains.DiffRequestChain] with the [ApplyCodeAction.Companion.VIRTUAL_FILE_PATH_LIST] key.
 *
 * @see [com.jetbrains.edu.learning.actions.CompareWithAnswerAction]
 */
class ApplyCodeAction : DumbAwareAction(), CustomComponentAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isUserDataPresented() && !e.isNextStepHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (!getConfirmationFromDialog(project)) return

    val diffRequestChain = e.getDiffRequestChain() ?: return showApplyCodeFailedNotification(project)
    val fileNames = diffRequestChain.getUserData(VIRTUAL_FILE_PATH_LIST).takeIf { !it.isNullOrEmpty() } ?: return showApplyCodeFailedNotification(project)

    if (tryApplyTexts(project, diffRequestChain, fileNames, ACTION_ID, this.templatePresentation.text)) {
      project.invokeLater {
        project.closeLastActiveFileEditor(e)
      }
      showApplyCodeSuccessfulNotification(project)
    } else {
      showApplyCodeFailedNotification(project)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent = object : ActionButton(
    this, presentation, place, JBUI.size(COMPONENT_SIZE)
  ) {
    init {
      val gotItTooltip = GotItTooltip(
        GOT_IT_ID, EduCoreBundle.message("action.Educational.Student.ApplyCode.tooltip.text")
      ).withHeader(EduCoreBundle.message("action.Educational.Student.ApplyCode.tooltip.title"))

      this.addComponentListener(object : ComponentAdapter() {
        override fun componentMoved(e: ComponentEvent) {
          val jComponent = e.component as JComponent
          if (jComponent.visibleRect.isEmpty) {
            gotItTooltip.hidePopup()
          }
          else {
            gotItTooltip.show(jComponent, GotItTooltip.BOTTOM_MIDDLE)
          }
        }
      })
    }
  }

  private fun AnActionEvent.isUserDataPresented() = getDiffRequestChain()?.getUserData(VIRTUAL_FILE_PATH_LIST) != null

  private fun getConfirmationFromDialog(project: Project): Boolean = when (Messages.showYesNoDialog(
    project,
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.text"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.title"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.yes.text"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.no.text"),
    AllIcons.General.Warning
  )) {
    Messages.YES -> true
    else -> false
  }

  private fun AnActionEvent.getDiffRequestChain(): DiffRequestChain? {
    val chainDiffVirtualFile = getData(CommonDataKeys.VIRTUAL_FILE) as? ChainDiffVirtualFile
    return chainDiffVirtualFile?.chain
  }

  private fun Project.closeDiffWindow(e: AnActionEvent) {
    val fileEditorManager = FileEditorManager.getInstance(this)
    val fileEditor = e.getData(LAST_ACTIVE_FILE_EDITOR) ?: return
    fileEditorManager.closeFile(fileEditor.file)
  }

  @Suppress("DialogTitleCapitalization")
  private fun showApplyCodeSuccessfulNotification(project: Project) {
    EduNotificationManager.showInfoNotification(
      project,
      EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.success.title"),
      EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.success.text"),
    )
  }
  private fun showApplyCodeFailedNotification(project: Project) {
    EduNotificationManager.showErrorNotification(
      project,
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.failed.title"),
      EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.failed.text")
    )
  }

  companion object {
    val VIRTUAL_FILE_PATH_LIST: Key<List<String>> = Key.create("virtualFilePathList")

    @NonNls
    const val ACTION_ID: String = "Educational.Student.ApplyCode"

    @NonNls
    private const val GOT_IT_ID: String = "diff.toolbar.apply.code.action.button"

    private const val COMPONENT_SIZE: Int = 22
  }
}