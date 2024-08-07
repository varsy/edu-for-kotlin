package com.jetbrains.edu.jarvis.highlighting.descriptiontocode

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.edu.jarvis.highlighting.HighlighterManager
import com.jetbrains.edu.jarvis.highlighting.ListenerManager


/**
 * Class DescriptionToCodeHighlighter is responsible for highlighting the description and code lines
 * in an editor based on the mouse movement.
 *
 * @property project The current project.
 * @property descriptionHighlighters The list of description highlighters.
 * @property codeHighlighters The list of code highlighters.
 * @property highlighterManager The instance of HighlighterManager.
 */
class DescriptionToCodeHighlighter(private val project: Project) {

  private val descriptionHighlighters = mutableListOf<RangeHighlighter>()
  private val codeHighlighters = mutableListOf<RangeHighlighter>()
  private val highlighterManager = HighlighterManager.getInstance(project)


  fun setUp(
    descriptionOffset: Int,
    draftOffset: Int,
    descriptionToCodeLines: Map<Int, List<Int>>,
    codeLineOffset: Int,
  ) = object : EditorMouseMotionListener {
    override fun mouseMoved(e: EditorMouseEvent) {
      val editor = e.editor
      val descriptionLineWithOffset = editor.xyToLogicalPosition(e.mouseEvent.point).line
      val descriptionLineOffset = editor.document.getLineNumber(descriptionOffset)
      val descriptionLine = descriptionLineWithOffset - descriptionLineOffset
      val descriptionLinesWithOffset = listOf(descriptionLineWithOffset)

      val draftLineOffset = editor.document.getLineNumber(draftOffset)

      descriptionToCodeLines[descriptionLine]?.map {
        it + draftLineOffset + codeLineOffset
      }?.let { codeLinesWithOffset ->
        setHighlighters(descriptionLinesWithOffset, codeLinesWithOffset)
      }
    }
  }.also {
    ListenerManager.getInstance(project).addListener(it)
  }

  private fun setHighlighters(descriptionLines: List<Int>, codeLines: List<Int>) {
    descriptionHighlighters.clearAndDispose()
    codeHighlighters.clearAndDispose()
    descriptionHighlighters.addHighlighters(descriptionLines)
    codeHighlighters.addHighlighters(codeLines)
  }

  private fun MutableList<RangeHighlighter>.clearAndDispose() {
    forEach { it.dispose() }
    clear()
  }

  private fun MutableList<RangeHighlighter>.addHighlighters(lines: List<Int>) = try {
    lines.forEach { line ->
      highlighterManager.addLineHighlighter(line, attributes)?.let { highlighter ->
        add(highlighter)
      }
    }
  } catch (_: IndexOutOfBoundsException) {
    // The code hasn't been generated yet.
  }

  companion object {
    private val attributes =
      TextAttributes().apply { backgroundColor = JBColor.LIGHT_GRAY }
  }
}
