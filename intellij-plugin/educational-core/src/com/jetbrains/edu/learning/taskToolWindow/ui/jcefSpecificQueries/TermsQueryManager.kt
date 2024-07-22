package com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.ui.GotItTooltip
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.taskToolWindow.ui.JsEventData
import com.jetbrains.edu.learning.theoryLookup.TermsManager
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.Point

/**
 * Manages tooltips that display definitions of terms.
 *
 * Adds a mouseover event listener to all the terms (displayed as span elements with a specific style).
 * When a user hovers over a term, a tooltip containing the definition of the term is displayed.
 * Also adds a scroll listener to close the tooltip when a user scrolls the page.
 */
class TermsQueryManager(
  private val project: Project,
  private val taskJBCefBrowser: JBCefBrowserBase
) : Disposable {
  private val jsQueryTermListener = JBCefJSQuery.create(taskJBCefBrowser)
  private val jsQueryScrollListener = JBCefJSQuery.create(taskJBCefBrowser)
  private var gotItTooltip: GotItTooltip? = null

  private val termListenerLoadHandler = object : CefLoadHandlerAdapter() {

    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      super.onLoadEnd(browser, frame, httpStatusCode)

      browser?.mainFrame?.executeJavaScript(
        """
          let terms = document.querySelectorAll('span[style*="border-bottom: 1px dashed gray;"]');
          [].slice.call(terms).forEach(term => {
            term.addEventListener('mouseover', function (event) {
              let boundingRect = term.getBoundingClientRect();
              let data = { 
                term: term.innerText, 
                x: event.clientX, 
                y: boundingRect.top
              };
              ${jsQueryTermListener.inject("JSON.stringify(data)")}
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
    jsQueryTermListener.addHandler { data ->
      showDefinitionOfTerm(data)
      null
    }
    jsQueryScrollListener.addHandler {
      gotItTooltip?.hidePopup()
      gotItTooltip = null
      null
    }
    Disposer.register(this, jsQueryTermListener)
    Disposer.register(this, jsQueryScrollListener)
    taskJBCefBrowser.jbCefClient.addLoadHandler(termListenerLoadHandler, taskJBCefBrowser.cefBrowser)
  }

  private fun showDefinitionOfTerm(data: String) {
    if (data.isBlank()) return
    if (gotItTooltip != null) return
    try {
      val parsedData = JsEventData.fromJson(data) ?: return
      val term = parsedData.term

      val task = project.selectedTaskFile?.task ?: return
      val termsManager = TermsManager.getInstance(project)
      val definition = termsManager.getTerms(task)[term] ?: return

      gotItTooltip = GotItTooltip(term, definition,  this)
        .withHeader(term)
        .withPosition(Balloon.Position.above)
        .withGotItButtonAction {
          gotItTooltip?.hidePopup()
          gotItTooltip = null
        }.apply {
          taskJBCefBrowser.component?.let {
            if (this.canShow()) {
              this.show(it) { _, _ -> Point(parsedData.x, parsedData.y) }
            }
          }
        }
    } catch (ignored: NumberFormatException) { }
  }

  override fun dispose() {
    taskJBCefBrowser.jbCefClient.removeLoadHandler(termListenerLoadHandler, taskJBCefBrowser.cefBrowser)
  }
}
