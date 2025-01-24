package com.jetbrains.edu.cognifire.highlighting.prompttocode

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.highlighting.GuardedBlockManager
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.cognifire.highlighting.ListenerManager
import com.jetbrains.edu.cognifire.highlighting.highlighers.LinkingHighlighter
import com.jetbrains.edu.cognifire.highlighting.highlighers.UncommittedChangesHighlighter
import com.jetbrains.edu.cognifire.highlighting.ProdeStateManager
import com.jetbrains.edu.cognifire.highlighting.highlighers.GuardBlockHighlighter
import com.jetbrains.edu.cognifire.models.BaseProdeExpression
import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.learning.selectedEditor
import kotlin.math.abs


/**
 * Class [PromptToCodeHighlighter] is responsible for highlighting the prompt and code lines
 * in an editor based on the mouse movement.
 *
 * @property project The current project.
 * @property highlighterManager The instance of [HighlighterManager].
 * @property listenerManager The instance of [ListenerManager].
 *
 */
class PromptToCodeHighlighter(private val project: Project, private val prodeId: String) {

  private val highlighterManager = HighlighterManager.getInstance()
  private val listenerManager = ListenerManager.getInstance(project)
  private var initialPromptContent: String = ""
  private var initialCodeContent: String = ""
  private val prodeStateManager = ProdeStateManager.getInstance()
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
    val document = project.selectedEditor?.document ?: error("Inconsistent document state")
    initialPromptContent = getContent(document, promptExpression)
    initialCodeContent = getContent(document, codeExpression)

    addMotionListener(promptExpression, codeExpression, promptToCodeLines, codeToPromptLines)
    addDocumentListener(promptExpression, codeExpression)

    prodeStateManager.addProde(
      prodeId,
      promptToCodeLines,
      codeToPromptLines,
      initialPromptContent,
      initialCodeContent
    )
    prodeStateManager.addMouseMotionListener(prodeId)
  }

  fun setUpListeners(promptExpression: PromptExpression, codeExpression: CodeExpression) =
    prodeStateManager.getProdeData(prodeId)?.let { prodeData ->
      with(prodeData) {
        initialPromptContent = initialPrompt
        initialCodeContent = initialCode
        addDocumentListener(promptExpression, codeExpression)
        if (prodeStateManager.hasMouseMotion(prodeId)) {
          addMotionListener(promptExpression, codeExpression, promptToCodeLines, codeToPromptLines)
        }
      }
    } ?: run {
      setUpDocumentListener(promptExpression, codeExpression)
    }

  private fun setUpDocumentListener(
    promptExpression: PromptExpression,
    codeExpression: CodeExpression
  ) {
    val document = project.selectedEditor?.document ?: error("Inconsistent document state")
    initialPromptContent = getContent(document, promptExpression)
    initialCodeContent = getContent(document, codeExpression)
    addDocumentListener(promptExpression, codeExpression)
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

      highlighterManager.clearProdeHighlighters<LinkingHighlighter>(prodeId)

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

  private fun getDocumentListener(promptExpression: PromptExpression, codeExpression: CodeExpression) = object : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      if (project.isDisposed) return
      val delta = event.newLength - event.oldLength
      val offset = event.offset

      val eventRange = offset until offset + abs(delta)

      val promptRange = promptExpression.startOffset until promptExpression.endOffset
      val codeRange = codeExpression.startOffset until codeExpression.endOffset


      val prodeIsEdited = eventRange.intersect(promptRange).isNotEmpty() ||
                          eventRange.intersect(codeRange).isNotEmpty()


      if (prodeIsEdited) {
        clearHighlighters()
        if (delta > 0) handleUncommitedChanges(offset, delta)
        if (delta != 0) handleReadOnlyBlocks(codeExpression, promptExpression, event)
      }
    }
  }

  private fun clearHighlighters() {
    highlighterManager.clearProdeHighlighters<LinkingHighlighter>(prodeId)
    listenerManager.clearAllMouseMotionListeners(prodeId)
    prodeStateManager.removeMouseMotionListener(prodeId)
  }

  private fun handleUncommitedChanges(offset: Int, delta: Int) {
    highlighterManager.addProdeHighlighter(UncommittedChangesHighlighter(offset, offset + delta), prodeId, project)
  }

  private fun addMotionListener(
    promptExpression: PromptExpression,
    codeExpression: CodeExpression,
    promptToCodeLines: Map<Int, List<Int>>,
    codeToPromptLines: Map<Int, List<Int>>
  ) {
    listenerManager.addListener(
      getMouseMotionListener(
        promptExpression,
        codeExpression,
        promptToCodeLines,
        codeToPromptLines
      ),
      prodeId
    )
  }
  
  private fun addDocumentListener(promptExpression: PromptExpression, codeExpression: CodeExpression) {
    listenerManager.addListener(
      getDocumentListener(promptExpression, codeExpression),
      prodeId
    )
  }

  private fun getContent(document: Document, prodeExpression: BaseProdeExpression) =
    document.charsSequence.slice(
      prodeExpression.startOffset until prodeExpression.endOffset
    ).toString().normalize()

  private fun String.normalize() = this.replace(Regex("\\s+"), " ").trim()

  private fun handleReadOnlyBlocks(codeExpression: CodeExpression, promptExpression: PromptExpression, event: DocumentEvent) {
    val document = event.document
    val currentPromptContent = getContent(document, promptExpression)
    val currentCodeContent = getContent(document, codeExpression)
    val guardManager = GuardedBlockManager.getInstance()
    highlighterManager.clearProdeHighlighters<GuardBlockHighlighter>(prodeId)

    when {
      event.offset in promptExpression.startOffset until promptExpression.endOffset
      && currentPromptContent != initialPromptContent -> addGuardedBlock(
        document,
        codeExpression,
        prodeId,
        guardManager
      )

      event.offset in codeExpression.startOffset until codeExpression.endOffset
      && currentCodeContent != initialCodeContent -> addGuardedBlock(
        document,
        promptExpression,
        prodeId,
        guardManager
      )

      else -> guardManager.removeGuardedBlock(prodeId, document)
    }
  }

  private fun addGuardedBlock(document: Document, expression: BaseProdeExpression, prodeId: String, guardManager: GuardedBlockManager) {
    with(expression) {
      highlighterManager.addProdeHighlighter(GuardBlockHighlighter(startOffset, endOffset), prodeId, project)
      guardManager.addGuardedBlock(document, startOffset, endOffset, prodeId)
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
      highlighterManager.addProdeHighlighter(LinkingHighlighter(line), prodeId, project)
    }
  }
  catch (_: IndexOutOfBoundsException) {
    // The code hasn't been generated yet.
  }
}
