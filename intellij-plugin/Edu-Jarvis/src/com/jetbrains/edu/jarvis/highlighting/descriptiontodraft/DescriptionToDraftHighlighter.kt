package com.jetbrains.edu.jarvis.highlighting.descriptiontodraft

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.edu.jarvis.highlighting.HighlighterManager
import com.jetbrains.edu.jarvis.highlighting.ListenerManager


/**
 * Class DescriptionToDraftHighlighter is responsible for highlighting the description and draft lines
 * in an editor based on the mouse movement.
 *
 * @property project The current project.
 * @property descriptionHighlighters The list of description highlighters.
 * @property draftHighlighters The list of draft highlighters.
 * @property highlighterManager The instance of HighlighterManager.
 */
class DescriptionToDraftHighlighter(private val project: Project) {

  private val descriptionHighlighters = mutableListOf<RangeHighlighter>()
  private val draftHighlighters = mutableListOf<RangeHighlighter>()
  private val highlighterManager = HighlighterManager.getInstance(project)

  fun setUp(
    descriptionOffset: Int,
    descriptionToDraftLines: Map<Int, List<Int>>,
    draftToDescriptionLines: Map<Int, List<Int>>,
    draftBodyOffset: Int,
  ) = object : EditorMouseMotionListener {
    override fun mouseMoved(e: EditorMouseEvent) {
      val editor = e.editor
      val selectedLineWithOffset = editor.xyToLogicalPosition(e.mouseEvent.point).line

      val descriptionLineOffset = editor.document.getLineNumber(descriptionOffset)
      val draftLineOffset = editor.document.getLineNumber(draftBodyOffset)


      showHighlighters(
        selectedLineWithOffset - descriptionLineOffset,
        descriptionLineOffset,
        draftLineOffset,
        descriptionToDraftLines,
        draftToDescriptionLines,
        true
      )
      showHighlighters(
        selectedLineWithOffset - draftLineOffset,
        draftLineOffset,
        descriptionLineOffset,
        draftToDescriptionLines,
        descriptionToDraftLines,
        false
      )
    }
  }.also {
    ListenerManager.getInstance(project).addListener(it)
  }

  private fun showHighlighters(
    fromLine: Int,
    fromLineOffset: Int,
    toLineOffset: Int,
    fromMap: Map<Int, List<Int>>,
    toMap: Map<Int, List<Int>>,
    descriptionToDraft: Boolean
  ) {
    fromMap[fromLine]?.map { it + toLineOffset }
      ?.let { fromLinesWithOffset ->
        val lines = fromMap[fromLine]?.let { toMap[it.singleOrNull()] }
                    ?: listOf(fromLine)
        val toLinesWithOffset = lines.map { it + fromLineOffset }
        if (descriptionToDraft) setHighlighters(fromLinesWithOffset, toLinesWithOffset)
        else setHighlighters(toLinesWithOffset, fromLinesWithOffset)
      }
  }


  private fun setHighlighters(descriptionLines: List<Int>, draftLines: List<Int>) {
    descriptionHighlighters.clearAndDispose()
    draftHighlighters.clearAndDispose()
    descriptionHighlighters.addHighlighters(descriptionLines)
    draftHighlighters.addHighlighters(draftLines)
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
  }
  catch (_: IndexOutOfBoundsException) {
    // The code hasn't been generated yet.
  }

  companion object {
    private val attributes =
      TextAttributes().apply { backgroundColor = JBColor.LIGHT_GRAY }
  }
}
