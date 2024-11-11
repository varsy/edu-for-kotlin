package com.jetbrains.edu.learning.aiDebugging.breakpoint

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.ui.DebuggerColors
import com.jetbrains.edu.learning.aiDebugging.breakpoint.AIBreakPointService.Companion.HIGHLIGHTING_COLOR
import com.jetbrains.edu.learning.getEditor

class AIBreakpointListener(private val project: Project) : XBreakpointListener<XLineBreakpoint<XBreakpointProperties<*>>> {

  private val highlighterRangers = mutableMapOf<XBreakpoint<out XBreakpointProperties<*>>, RangeHighlighter>()

  override fun breakpointAdded(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
    super.breakpointAdded(breakpoint)
    highlightBreakpoint(breakpoint)
  }

  override fun breakpointRemoved(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
    val position = breakpoint.sourcePosition ?: error("There are no position for the breakpoint")
    val range = highlighterRangers[breakpoint]
    range?.let {
      position.file.getEditor(project)?.markupModel?.removeHighlighter(it)
    }
    super.breakpointRemoved(breakpoint)
  }


  /**
   * Highlights a given breakpoints line in the editor with a specific AI background color.
   *
   * @param breakpoint The breakpoint that needs to be highlighted. It contains the line and file details.
   */
  fun highlightBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
    val position = breakpoint.sourcePosition ?: error("There are no position for the breakpoint")
    ApplicationManager.getApplication().invokeLater {
      val editor = position.file.getEditor(project) ?: return@invokeLater
      val attribute = TextAttributes().apply {
        backgroundColor = HIGHLIGHTING_COLOR
      }
      val range = editor.markupModel.addLineHighlighter(
        position.line,
        DebuggerColors.BREAKPOINT_HIGHLIGHTER_LAYER + 1,
        attribute
      )
      highlighterRangers[breakpoint] = range
    }
  }

}


