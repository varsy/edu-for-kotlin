package com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GotItTooltip
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.taskToolWindow.TERM_CLASS
import com.jetbrains.edu.learning.taskToolWindow.ui.JsEventData
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.jetbrains.annotations.NonNls
import java.awt.Point

/**
 * Manages tooltips that display definitions of terms.
 *
 * Adds a mouseover event listener to all the terms (displayed as span elements with a specific style).
 * When a user hovers over a term, a tooltip containing the definition of the term is displayed.
 * Also adds a scroll listener to close the tooltip when a user scrolls the page.
 */
// TODO: Implement an analogue for Swing
class TermsQueryManager private constructor(
  private val project: Project,
  private val taskJBCefBrowser: JBCefBrowserBase
) : Disposable {
  private val jsQueryMouseOverListener = JBCefJSQuery.create(taskJBCefBrowser)
  private val jsQueryMouseOutListener = JBCefJSQuery.create(taskJBCefBrowser)
  private val jsQueryScrollListener = JBCefJSQuery.create(taskJBCefBrowser)
  private var gotItTooltip: GotItTooltip? = null

  private val termListenerLoadHandler = object : CefLoadHandlerAdapter() {

    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      super.onLoadEnd(browser, frame, httpStatusCode)

      browser?.mainFrame?.executeJavaScript(
        """
          let terms = document.getElementsByClassName('$TERM_CLASS');
          [].slice.call(terms).forEach(term => {
            term.addEventListener('mouseover', function (event) {
              let boundingRect = term.getBoundingClientRect();
              let data = { 
                term: term.innerText, 
                x: event.clientX, 
                y: event.clientY, 
                bottomOfTermRect: boundingRect.bottom,
                topOfTermRect: boundingRect.top
              };
              ${jsQueryMouseOverListener.inject("JSON.stringify(data)")}
            });
            term.addEventListener('mouseout', function (event) {
              let data = { 
                term: term.innerText, 
                x: event.clientX, 
                y: event.clientY
              };
              ${jsQueryMouseOutListener.inject("JSON.stringify(data)")}
            });
          });
          window.addEventListener('scroll', function() { 
             ${jsQueryScrollListener.inject("window.scrollY")}
          });
          """.trimIndent(), taskJBCefBrowser.cefBrowser.url, 0
      )
    }
  }

  init {
    jsQueryMouseOverListener.addHandler { data ->
      showDefinitionOfTerm(data)
      null
    }
    jsQueryMouseOutListener.addHandler { data ->
      closeDefinitionOfTerm(data)
      null
    }
    jsQueryScrollListener.addHandler {
      gotItTooltip?.dispose()
      null
    }
    Disposer.register(this) {
      Disposer.dispose(jsQueryMouseOverListener)
      Disposer.dispose(jsQueryMouseOutListener)
      Disposer.dispose(jsQueryScrollListener)
    }
    taskJBCefBrowser.jbCefClient.addLoadHandler(termListenerLoadHandler, taskJBCefBrowser.cefBrowser)
  }

  private fun showDefinitionOfTerm(data: String) {
    if (data.isBlank()) return
    gotItTooltip?.dispose()
    val parsedData = JsEventData.fromJson(data) ?: return
    val component = taskJBCefBrowser.component ?: return
    val termTitle = parsedData.term

    val task = project.selectedTaskFile?.task ?: return
    val definition = TermsProjectSettings.getInstance(project).getTaskTerms(task)?.find { it.value == termTitle }?.definition ?: return

    val isBelowMiddle = parsedData.y < component.height / 2
    val position = if (isBelowMiddle) Balloon.Position.below else Balloon.Position.above
    val pointY = if (isBelowMiddle) parsedData.bottomOfTermRect else parsedData.topOfTermRect

    gotItTooltip = GotItTooltip(TOOLTIP_ID, definition, this)
      .withHeader(termTitle)
      .withPosition(position)
      .withGotItButtonAction {
        gotItTooltip?.dispose()
      }.apply {
        if (canShow()) {
          show(component) { _, _ -> Point(parsedData.x, pointY ?: parsedData.y) }
        }
      }
  }

  private fun closeDefinitionOfTerm(data: String) {
    if (data.isBlank()) return
    // TODO: keep the gotItTooltip open when the mouse moves towards it
    gotItTooltip?.dispose()
  }

  override fun dispose() {
    gotItTooltip?.dispose()
    taskJBCefBrowser.jbCefClient.removeLoadHandler(termListenerLoadHandler, taskJBCefBrowser.cefBrowser)
  }

  companion object {
    @NonNls
    private const val TOOLTIP_ID: String = "term.definition"

    @JvmStatic
    fun getTermsQueryManager(project: Project, task: Task?, taskJBCefBrowser: JBCefBrowserBase): TermsQueryManager? =
      when (task) {
        is TheoryTask -> TermsQueryManager(project, taskJBCefBrowser)
        else -> null
      }
  }
}
