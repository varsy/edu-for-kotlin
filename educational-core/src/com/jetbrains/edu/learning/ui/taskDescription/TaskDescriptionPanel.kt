package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.SeparatorComponent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckPanel
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.BoxLayout
import javax.swing.JPanel


class TaskDescriptionPanel : SimpleToolWindowPanel(true, true), DataProvider, Disposable {
  override fun dispose() {

  }

  val checkPanel: CheckPanel = CheckPanel()

  init {
    val defaultBackground = EditorColorsManager.getInstance().globalScheme.defaultBackground

    val label = JBLabel("<html>sample text<br><br><br>" +
                        "sample text<br><br><br>\n" +
                        "sample text\n" +
                        "sample text<br><br><br>\n" +
                        "sample text\n <br><br><br>" +
                        "<br><br><br>" +
                        "<br><br><br>" +
                        "<br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br><br>" +
                        "" +
                        "" +
                        "" +
                        "" +
                        "" +
                        "" +
                        "sample text\n" +
                        "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" + "sample text<br><br><br>\n" + "sample text\n" +
                        "sample text<br><br><br>\n" +
                        "sample text\n" +
                        "sample text\n" + "sample text\n" + "sample text\n" +
                        "sample text\n" +
                        "sample text\n" + "sample text\n</html>")
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)


    val jbScrollPane = JBScrollPane(label)
    panel.add(jbScrollPane)
    jbScrollPane.alignmentX = Component.LEFT_ALIGNMENT

    val jSeparator = SeparatorComponent()
    jSeparator.alignmentX = Component.LEFT_ALIGNMENT
    panel.add(jSeparator)

    val bottomPanel = JPanel(BorderLayout())

    bottomPanel.add(checkPanel, BorderLayout.NORTH)
    bottomPanel.alignmentX = Component.LEFT_ALIGNMENT

    panel.add(bottomPanel)

    UIUtil.setBackgroundRecursively(panel, defaultBackground)

    setContent(panel)
  }

  fun checkStarted() {
    checkPanel.checkStarted()
  }

  fun checkFinished(checkResult: CheckResult) {
    checkPanel.checkFinished(checkResult)
  }

  companion object {
    fun getToolWindow(project: Project): TaskDescriptionPanel? {
      if (project.isDisposed) return null

      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
      if (toolWindow != null) {
        val contents = toolWindow.contentManager.contents
        for (content in contents) {
          val component = content.component
          if (component is TaskDescriptionPanel) {
            return component
          }
        }
      }
      return null
    }
  }
}