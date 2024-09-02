package com.jetbrains.edu.learning.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.GotItTooltip
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
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
 * that is used in the [com.intellij.diff.chains.DiffRequestChain] with the [ApplyCodeActionBase.Companion.VIRTUAL_FILE_PATH_LIST] key.
 *
 * @see [com.jetbrains.edu.learning.actions.CompareWithAnswerAction]
 */
class ApplyCodeAction : ApplyCodeActionBase() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isUserDataPresented() && !e.isNextStepHintDiff()
  }

  override fun createCustomComponent(presentation: Presentation, place: String): JComponent =
    ApplyCodeActionButton(this, presentation, place)

  override fun getConfirmationFromDialog(project: Project): Boolean = Messages.showYesNoDialog(
    project,
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.text"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.title"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.yes.text"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.dialog.no.text"),
    AllIcons.General.Warning
  ) == Messages.YES

  override fun showSuccessfulNotification(project: Project) = EduNotificationManager.showInfoNotification(
    project,
    @Suppress("DialogTitleCapitalization") EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.success.title"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.success.text"),
  )

  override fun showFailedNotification(project: Project) = EduNotificationManager.showErrorNotification(
    project,
    @Suppress("DialogTitleCapitalization") EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.failed.title"),
    EduCoreBundle.message("action.Educational.Student.ApplyCode.notification.failed.text")
  )

  override val actionId: String = ACTION_ID

  private class ApplyCodeActionButton(
    action: AnAction, presentation: Presentation, place: String
  ) : ActionButton(action, presentation, place, JBUI.size(COMPONENT_SIZE)) {

    init {
      val gotItTooltip = GotItTooltip(
        GOT_IT_ID, EduCoreBundle.message("action.Educational.Student.ApplyCode.tooltip.text")
      ).withHeader(EduCoreBundle.message("action.Educational.Student.ApplyCode.tooltip.title"))

      addComponentListener(createComponentAdapter(gotItTooltip))
    }

    private fun createComponentAdapter(gotItTooltip: GotItTooltip) = object : ComponentAdapter() {
      override fun componentMoved(e: ComponentEvent) {
        val jComponent = e.component as JComponent
        gotItTooltip.update(jComponent)
      }
    }

    private fun GotItTooltip.update(jComponent: JComponent) = if (jComponent.visibleRect.isEmpty) {
      hidePopup()
    }
    else {
      show(jComponent, GotItTooltip.BOTTOM_MIDDLE)
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.Student.ApplyCode"

    @NonNls
    private const val GOT_IT_ID: String = "diff.toolbar.apply.code.action.button"

    private const val COMPONENT_SIZE: Int = 22
  }
}