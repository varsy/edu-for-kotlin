package com.jetbrains.edu.learning.taskToolWindow.ui.tab

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.InlineBanner
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.JCEFToolWindow
import com.jetbrains.edu.learning.taskToolWindow.ui.SwingToolWindow
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Constructor is called exclusively in [com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
class DescriptionTab(project: Project) : TaskToolWindowTab(project, TabType.DESCRIPTION_TAB) {

  private val taskTextToolWindow = if (JavaUILibrary.isJCEF()) {
    JCEFToolWindow(project)
  }
  else {
    SwingToolWindow(project)
  }

  private val inlineBannersPanel = JPanel(VerticalLayout(4))

  init {
    LOG.info("Description tab uses `${taskTextToolWindow.javaClass.name}` impl")

    Disposer.register(this, taskTextToolWindow)

    val taskDescription = taskTextToolWindow.taskInfoPanel
    taskDescription.border = JBUI.Borders.emptyBottom(10)
    taskDescription.add(inlineBannersPanel, BorderLayout.SOUTH)

    add(taskDescription, BorderLayout.CENTER)

    val bottomPanel = JPanel(BorderLayout())
    add(bottomPanel, BorderLayout.SOUTH)

    val taskSpecificPanel = taskTextToolWindow.taskSpecificPanel
    taskSpecificPanel.border = JBUI.Borders.emptyRight(15)
    bottomPanel.add(taskSpecificPanel, BorderLayout.CENTER)
  }

  fun updateTaskSpecificPanel(task: Task?) {
    taskTextToolWindow.updateTaskSpecificPanel(task)
  }

  override fun update(task: Task) {
    taskTextToolWindow.setTaskText(task)
  }

  fun addInlineBanner(inlineBanner: InlineBanner) {
    inlineBannersPanel.add(inlineBanner)
  }

  companion object {
    private val LOG = logger<DescriptionTab>()
  }
}