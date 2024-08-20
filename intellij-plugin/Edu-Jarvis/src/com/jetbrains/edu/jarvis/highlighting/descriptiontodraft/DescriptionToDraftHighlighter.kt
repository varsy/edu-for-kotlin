package com.jetbrains.edu.jarvis.highlighting.descriptiontodraft

import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
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
 * @property highlighterManager The instance of HighlighterManager.
 */
class DescriptionToDraftHighlighter(private val project: Project) {

  private val highlighterManager = HighlighterManager.getInstance(project)

  fun setUp(
    descriptionOffset: Int,
    descriptionToDraftLines: Map<Int, List<Int>>,
    draftToDescriptionLines: Map<Int, List<Int>>,
    draftBodyOffset: Int,
  ) = object : EditorMouseMotionListener {
    override fun mouseMoved(e: EditorMouseEvent)
    {
      val editor = e.editor
      val selectedLineWithOffset = editor.xyToLogicalPosition(e.mouseEvent.point).line

      val descriptionLineOffset = editor.document.getLineNumber(descriptionOffset)
      val draftLineOffset = editor.document.getLineNumber(draftBodyOffset)

      highlighterManager.clearDescriptionToDraftHighlighters()

      showHighlighters(
        selectedLineWithOffset - descriptionLineOffset,
        draftLineOffset,
        descriptionLineOffset,
        descriptionToDraftLines,
        draftToDescriptionLines
      )

      showHighlighters(
        selectedLineWithOffset - draftLineOffset,
        descriptionLineOffset,
        draftLineOffset,
        draftToDescriptionLines,
        descriptionToDraftLines
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
  ) {
    val fromLines = fromMap[fromLine] ?: return
    val fromLinesWithOffset = fromLines.map { it + fromLineOffset }
    val toLines = toMap[fromLines.singleOrNull()] ?: listOf(fromLine)
    val toLinesWithOffset = toLines.map { it + toLineOffset }

    addHighlighters(fromLinesWithOffset + toLinesWithOffset)
  }

  private fun addHighlighters(lines: List<Int>) = try {
    lines.forEach { line ->
      highlighterManager.addDescriptionToDraftHighlighter(line, attributes)
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
