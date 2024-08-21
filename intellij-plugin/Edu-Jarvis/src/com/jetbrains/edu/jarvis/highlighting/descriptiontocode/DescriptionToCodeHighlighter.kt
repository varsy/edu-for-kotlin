package com.jetbrains.edu.jarvis.highlighting.descriptiontocode

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.edu.jarvis.highlighting.HighlighterManager
import com.jetbrains.edu.jarvis.highlighting.ListenerManager
import com.jetbrains.edu.jarvis.models.DescriptionExpression
import com.jetbrains.edu.jarvis.models.DraftExpression

/**
 * Class DescriptionToDraftHighlighter is responsible for highlighting the description and draft lines
 * in an editor based on the mouse movement.
 *
 * @property project The current project.
 * @property highlighterManager The instance of [HighlighterManager].
 * @property listenerManager The instance of [ListenerManager].
 *
 */
class DescriptionToCodeHighlighter(private val project: Project) {

  private val highlighterManager = HighlighterManager.getInstance(project)
  private val listenerManager = ListenerManager.getInstance(project)

  /**
   * Sets up the EditorMouseMotionListener to handle mouse movement events in the editor.
   * The method creates an anonymous class implementation of the EditorMouseMotionListener interface
   *  and registers it with the ListenerManager. The listener handles the logic for highlighting lines of code based on the mouse position.
   *
   * @param descriptionExpression Represents the `description` block.
   * @param draftExpression Represents the `draft` block.
   * @param descriptionToDraftLines A map that contains the line numbers in the description section as the keys
   * and a list of corresponding line numbers in the draft section as the values.
   * @param draftToDescriptionLines A map that contains the line numbers in the draft section as the keys
   * and a list of corresponding line numbers in the description section as the values.
   */
  fun setUp(
    descriptionExpression: DescriptionExpression,
    draftExpression: DraftExpression,
    descriptionToDraftLines: Map<Int, List<Int>>,
    draftToDescriptionLines: Map<Int, List<Int>>
  ) {
    ListenerManager.getInstance(project).addListener(
      getMouseMotionListener(
        descriptionExpression.promptOffset,
        draftExpression.codeOffset,
        descriptionToDraftLines,
        draftToDescriptionLines
      )
    )
    ListenerManager.getInstance(project).addListener(
      getDocumentListener(draftExpression)
    )
  }

  private fun getMouseMotionListener(
    descriptionOffset: Int,
    draftBodyOffset: Int,
    descriptionToDraftLines: Map<Int, List<Int>>,
    draftToDescriptionLines: Map<Int, List<Int>>,
  ) = object : EditorMouseMotionListener {
    override fun mouseMoved(e: EditorMouseEvent) {
      val editor = e.editor
      val selectedLineWithOffset = editor.xyToLogicalPosition(e.mouseEvent.point).line

      val descriptionLineOffset = editor.document.getLineNumber(descriptionOffset)
      val draftLineOffset = editor.document.getLineNumber(draftBodyOffset)

      highlighterManager.clearDescriptionToDraftHighlighters()

      if(selectedLineWithOffset - descriptionLineOffset in descriptionToDraftLines.keys) showHighlighters(
        selectedLineWithOffset - descriptionLineOffset,
        descriptionLineOffset,
        draftLineOffset,
        descriptionToDraftLines,
        draftToDescriptionLines
      )
      else if (selectedLineWithOffset - draftLineOffset in draftToDescriptionLines.keys) showHighlighters(
        selectedLineWithOffset - draftLineOffset,
        draftLineOffset,
        descriptionLineOffset,
        draftToDescriptionLines,
        descriptionToDraftLines
      )
    }
  }

  private fun getDocumentListener(draftExpression: DraftExpression) = object: DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      if(event.offset !in draftExpression.startOffset..draftExpression.endOffset) return

      highlighterManager.clearAll()
      listenerManager.clearAll()
    }
  }

  private fun showHighlighters(
    originLine: Int,
    originLineOffset: Int,
    destinationLineOffset: Int,
    originToDestination: Map<Int, List<Int>>,
    destinationToOrigin: Map<Int, List<Int>>,
  ) {
    val destinationLines = originToDestination[originLine] ?: return
    val destinationLinesWithOffset = destinationLines.map { it + destinationLineOffset }
    val originLines = destinationToOrigin[destinationLines.singleOrNull()] ?: listOf(originLine)
    val originLinesWithOffset = originLines.map { it + originLineOffset }

    addHighlighters(destinationLinesWithOffset + originLinesWithOffset)
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
