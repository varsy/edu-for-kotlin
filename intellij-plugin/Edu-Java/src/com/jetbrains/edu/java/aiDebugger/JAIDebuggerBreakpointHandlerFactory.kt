package com.jetbrains.edu.java.aiDebugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaBreakpointHandler
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.TextRange
import com.intellij.ui.GotItTooltip
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.jetbrains.edu.learning.selectedEditor

class JAIDebuggerBreakpointHandlerFactory : JavaBreakpointHandlerFactory {
  override fun createHandler(process: DebugProcessImpl): JavaBreakpointHandler =
    object : JavaBreakpointHandler(JAIDebuggerBreakpointType::class.java, process) {
      override fun registerBreakpoint(breakpoint: XBreakpoint<out XBreakpointProperties<*>>) {
        super.registerBreakpoint(breakpoint)
        breakpoint.sourcePosition?.let {
          invokeLater {
            showTooltipAboveLine(process.project, it.line)
          }
        }
      }
    }

  // TODO Replace with the usage of a panel for notifications
  fun showTooltipAboveLine(
    project: Project,
    lineNumber: Int
  ) {
    val selectedEditor = project.selectedEditor ?: return
    val document = selectedEditor.document
    if (lineNumber < 0 || lineNumber >= document.lineCount) {
      return
    }
    val lineStartOffset = document.getLineStartOffset(lineNumber)
    val lineEndOffset = document.getLineEndOffset(lineNumber)
    val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset)).trim()
    val middleOffset = lineText.length / 2
    val middleTextOffset = lineStartOffset + lineText.indexOf(lineText[middleOffset])
    val point = selectedEditor.visualPositionToXY(selectedEditor.offsetToVisualPosition(middleTextOffset))
    val editorComponent = selectedEditor.contentComponent
    val tooltip = GotItTooltip("AI_DEBUGGER", "TEXT").withPosition(Balloon.Position.above)
    tooltip.show(editorComponent) { _, _ -> point }
  }
}
