package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefClient
import com.intellij.ui.jcef.JCEFHtmlPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.links.JCefToolWindowLinkHandler
import com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries.TaskQueryManager
import com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries.TermsQueryManager
import org.jetbrains.annotations.TestOnly
import javax.swing.JComponent

class JCEFToolWindow(project: Project) : TaskToolWindow(project) {
  private val taskInfoJBCefBrowser = JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null)
  private val taskSpecificJBCefBrowser = JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null)
  private var taskSpecificQueryManager: TaskQueryManager<out Task>? = null
  private var termsQueryManager: TermsQueryManager? = null

  init {
    val jcefLinkInToolWindowHandler = JCefToolWindowLinkHandler(project)
    val taskInfoRequestHandler = JCEFToolWindowRequestHandler(jcefLinkInToolWindowHandler)
    taskInfoJBCefBrowser.jbCefClient.addRequestHandler(taskInfoRequestHandler, taskInfoJBCefBrowser.cefBrowser)
    val taskInfoLifeSpanHandler = JCEFTaskInfoLifeSpanHandler(jcefLinkInToolWindowHandler)
    taskInfoJBCefBrowser.jbCefClient.addLifeSpanHandler(taskInfoLifeSpanHandler, taskInfoJBCefBrowser.cefBrowser)
    taskSpecificJBCefBrowser.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, TASK_SPECIFIC_PANEL_JS_QUERY_POOL_SIZE)

    taskInfoJBCefBrowser.disableNavigation()
    Disposer.register(this, taskInfoJBCefBrowser)
    Disposer.register(this, taskSpecificJBCefBrowser)

    termsQueryManager = TermsQueryManager(project, taskInfoJBCefBrowser)

    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(LafManagerListener.TOPIC,
                 LafManagerListener { TaskToolWindowView.updateAllTabs(project) })
  }

  override val taskInfoPanel: JComponent
    get() = taskInfoJBCefBrowser.component

  override val taskSpecificPanel: JComponent
    get() = taskSpecificJBCefBrowser.component

  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.JCEF

  override fun setText(text: String) {
    taskInfoJBCefBrowser.loadHTML(text)
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificJBCefBrowser.component.isVisible = false

    val taskText = getHTMLTemplateText(task) ?: return

    // Dispose taskSpecificQueryManager manually because this disposes existing JSQueries and removes them from JS_QUERY_POOL
    taskSpecificQueryManager?.let {
      Disposer.dispose(it)
    }
    termsQueryManager?.let {
      Disposer.dispose(it)
    }

    taskSpecificQueryManager = getTaskSpecificQueryManager(task, taskSpecificJBCefBrowser)
    termsQueryManager = TermsQueryManager(project, taskInfoJBCefBrowser)

    taskSpecificJBCefBrowser.component.preferredSize = JBUI.size(Int.MAX_VALUE, 250)
    val html = htmlWithResources(project, taskText, task)
    taskSpecificJBCefBrowser.loadHTML(html)
    taskSpecificJBCefBrowser.component.isVisible = true
  }

  override fun dispose() {
    super.dispose()
    // Dispose undisposed yet taskSpecificQueryManager
    taskSpecificQueryManager?.let {
      Disposer.dispose(it)
    }
    termsQueryManager?.let {
      Disposer.dispose(it)
    }
  }

  companion object {
    // maximum number of created qs queries in taskSpecificQueryManager
    private const val TASK_SPECIFIC_PANEL_JS_QUERY_POOL_SIZE = 2

    @TestOnly
    fun processContent(content: String, project: Project): String {
      return htmlWithResources(project, content)
    }
  }
}
