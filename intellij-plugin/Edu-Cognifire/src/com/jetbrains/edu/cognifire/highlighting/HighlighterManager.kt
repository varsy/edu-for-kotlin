package com.jetbrains.edu.cognifire.highlighting

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.highlighting.highlighers.ProdeHighlighter
import com.jetbrains.edu.learning.selectedEditor

/**
 * Manages the highlighters in the editor. Allows adding and clearing highlighters for specific ranges and lines of code.
 *
 * @param project The project in which the currently edited file is located.
 */
@Service(Service.Level.PROJECT)
class HighlighterManager(private val project: Project) {
  val highlighters = mutableListOf<ProdeHighlighter>()

  private val markupModel: MarkupModel?
    get() = project.selectedEditor?.markupModel

  fun clearAll() {
    highlighters.forEach {
      it.markupHighlighter?.dispose()
    }
    highlighters.clear()
  }

  inline fun <reified T : ProdeHighlighter> clearProdeHighlighters() {
    highlighters.filterIsInstance<T>().forEach {
      it.markupHighlighter?.dispose()
    }
    highlighters.removeAll { it is T }
  }

  fun addProdeHighlighter(highlighter: ProdeHighlighter) {
    highlighter.addMarkupHighlighter(markupModel)
    highlighters.add(highlighter)
  }

  companion object {
    fun getInstance(project: Project): HighlighterManager = project.service()
  }
}