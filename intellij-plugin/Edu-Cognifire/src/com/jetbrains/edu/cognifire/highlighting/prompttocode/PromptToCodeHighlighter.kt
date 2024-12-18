package com.jetbrains.edu.cognifire.highlighting.prompttocode

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.cognifire.highlighting.ListenerManager
import com.jetbrains.edu.cognifire.highlighting.highlighers.LinkingHighlighter
import com.jetbrains.edu.cognifire.highlighting.highlighers.UncommitedChangesHighlighter
import com.jetbrains.edu.cognifire.models.BaseProdeExpression
import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.models.PromptExpression

/**
 * Class [PromptToCodeHighlighter] is responsible for highlighting the prompt and code lines
 * in an editor based on the mouse movement.
 *
 * @property project The current project.
 * @property highlighterManager The instance of [HighlighterManager].
 * @property listenerManager The instance of [ListenerManager].
 *
 */
class PromptToCodeHighlighter(private val project: Project) {

  private val highlighterManager = HighlighterManager.getInstance(project)
  private val listenerManager = ListenerManager.getInstance(project)

  /**
   * Sets up the EditorMouseMotionListener to handle mouse movement events in the editor.
   * The method creates an anonymous class implementation of the EditorMouseMotionListener interface
   *  and registers it with the ListenerManager. The listener handles the logic for highlighting lines of code based on the mouse position.
   *
   * @param promptExpression Represents the `prompt` block.
   * @param codeExpression Represents the `code` block.
   * @param promptToCodeLines A map that contains the line numbers in the prompt section as the keys
   * and a list of corresponding line numbers in the code section as the values.
   * @param codeToPromptLines A map that contains the line numbers in the code section as the keys
   * and a list of corresponding line numbers in the prompt section as the values.
   */
  fun setUp(
    promptExpression: PromptExpression,
    codeExpression: CodeExpression,
    promptToCodeLines: Map<Int, List<Int>>,
    codeToPromptLines: Map<Int, List<Int>>
  ) {
    ListenerManager.getInstance(project).addListener(
      getMouseMotionListener(
        promptExpression,
        codeExpression,
        promptToCodeLines,
        codeToPromptLines
      )
    )
    ListenerManager.getInstance(project).addListener(
      getDocumentListener(codeExpression, promptExpression)
    )
  }

  private fun getMouseMotionListener(
    promptExpression: PromptExpression,
    codeExpression: CodeExpression,
    promptToCodeLines: Map<Int, List<Int>>,
    codeToPromptLines: Map<Int, List<Int>>,
  ) = object : EditorMouseMotionListener {
    override fun mouseMoved(e: EditorMouseEvent) {
      val editor = e.editor
      val selectedLineWithOffset = editor.xyToLogicalPosition(e.mouseEvent.point).line

      val promptLineOffset = editor.document.getLineNumber(promptExpression.contentOffset)
      val codeLineOffset = editor.document.getLineNumber(codeExpression.contentOffset)

      highlighterManager.clearProdeHighlighters<LinkingHighlighter>()

      if (selectedLineWithOffset - promptLineOffset in promptToCodeLines.keys) showHighlighters(
        selectedLineWithOffset - promptLineOffset,
        promptLineOffset,
        codeLineOffset,
        promptToCodeLines,
        codeToPromptLines
      )
      else if (selectedLineWithOffset - codeLineOffset in codeToPromptLines.keys) showHighlighters(
        selectedLineWithOffset - codeLineOffset,
        codeLineOffset,
        promptLineOffset,
        codeToPromptLines,
        promptToCodeLines
      )
    }
  }

  private fun getDocumentListener(codeExpression: CodeExpression, promptExpression: PromptExpression) = object : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      val delta = event.newLength - event.oldLength
      val offset = event.offset

      fun BaseProdeExpression.shiftOffsets(shiftStart: Boolean = false, shiftEnd: Boolean = false) {
        if (shiftStart) shiftStartOffset(delta)
        if (shiftEnd) shiftEndOffset(delta)
      }

      var prodeIsEdited = false

      when (offset) {
        in 0 until promptExpression.startOffset -> {
          promptExpression.shiftOffsets(shiftStart = true, shiftEnd = true)
          codeExpression.shiftOffsets(shiftStart = true, shiftEnd = true)
        }

        in promptExpression.startOffset until promptExpression.endOffset -> {
          promptExpression.shiftOffsets(shiftEnd = true)
          codeExpression.shiftOffsets(shiftStart = true, shiftEnd = true)
          prodeIsEdited = true
        }

        in promptExpression.endOffset until codeExpression.startOffset -> {
          codeExpression.shiftOffsets(shiftStart = true, shiftEnd = true)
        }

        in codeExpression.startOffset until codeExpression.endOffset -> {
          codeExpression.shiftOffsets(shiftEnd = true)
          prodeIsEdited = true
        }

      }

      if (prodeIsEdited && delta > 0) {
        clearHighlighters()
        handleUncommitedChanges(offset, delta)
      }
    }


  }

  private fun clearHighlighters() {
    highlighterManager.clearProdeHighlighters<LinkingHighlighter>()
    listenerManager.clearAllMouseMotionListeners()
  }

  private fun handleUncommitedChanges(offset: Int, delta: Int) {
    highlighterManager.addProdeHighlighter(
      UncommitedChangesHighlighter(offset, offset + delta)
    )
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
      highlighterManager.addProdeHighlighter(
        LinkingHighlighter(line)
      )
    }
  }
  catch (_: IndexOutOfBoundsException) {
    // The code hasn't been generated yet.
  }
}
