package com.jetbrains.edu.jarvis.highlighting

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.edu.learning.selectedEditor

@Service(Service.Level.PROJECT)
class HighlightingListenerManager(private val project: Project) {
  private val highlighterManager = HighlighterManager.getInstance(project)
  private var editorMouseMotionListener: EditorMouseMotionListener? = null

  private val editor: Editor?
    get() = project.selectedEditor

  fun setMouseMotionListener(
    descriptionOffset: Int,
    draftOffset: Int,
    descriptionToCodeLines: Map<Int, List<Int>>,
    codeLineOffset: Int
  ) {
    editorMouseMotionListener = object : EditorMouseMotionListener {
      override fun mouseMoved(e: EditorMouseEvent) {
        val selectedLine = e.editor.xyToLogicalPosition(e.mouseEvent.point).line
        val descriptionLineOffset = e.editor.document.getLineNumber(descriptionOffset)
        val draftLineOffset = e.editor.document.getLineNumber(draftOffset)

        val descriptionLine = selectedLine - descriptionLineOffset
        descriptionToCodeLines[descriptionLine]?.map {
          it + draftLineOffset + codeLineOffset
        }?.let { codeLines ->
          addHighlighters(selectedLine, codeLines)
        }
      }
    }.also {
      editor?.addEditorMouseMotionListener(it)
    }
  }

  fun clearMouseMotionListener() {
    editorMouseMotionListener?.let {
      editor?.removeEditorMouseMotionListener(it)
    }
  }

  private fun addHighlighters(descriptionLine: Int, codeLines: List<Int>) {
      highlighterManager.clearLineHighlighters()
      highlighterManager.addDescriptionLineHighlighter(descriptionLine, attributes)
      codeLines.forEach {
        highlighterManager.addCodeLineHighlighter(it, attributes)
      }
  }

  companion object {
    fun getInstance(project: Project): HighlightingListenerManager = project.service()
    private val attributes =
      TextAttributes().apply { backgroundColor = JBColor.YELLOW }
  }
}