package com.jetbrains.edu.learning.aiDebugging.breakpoint

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.*
import com.intellij.xdebugger.ui.DebuggerColors
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.getEditor
import java.awt.Color

@Service(Service.Level.PROJECT)
class AIBreakPointService(private val project: Project) {

  private val highlighterRangers = mutableMapOf<XBreakpoint<out XBreakpointProperties<*>>, RangeHighlighter>()

  private val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager

  fun initialize(){
    val language = Language.findLanguageByID(project.course?.languageId)
    language?.let {
      val breakpointType = BreakpointTypeManager.getInstance(it).getBreakPointType()
      val type = XDebuggerUtil.getInstance().findBreakpointType(breakpointType::class.java)
      breakpointManager.getBreakpoints(type).forEach(::highlightBreakpoint)
      addListener(type)
    }
  }


  fun toggleLineBreakpoint(language: Language, file: VirtualFile, line: Int) {
    val breakpointType = BreakpointTypeManager.getInstance(language).getBreakPointType()
    val type = XDebuggerUtil.getInstance().findBreakpointType(breakpointType::class.java)
    breakpointManager.addLineBreakpoint(type, file.url, line, type.createProperties())

  }

  private val listener by lazy {
    return@lazy object : XBreakpointListener<XLineBreakpoint<XBreakpointProperties<*>>> {
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
    }
  }

  private fun highlightBreakpoint(breakpoint: XLineBreakpoint<XBreakpointProperties<*>>) {
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

  private fun addListener(type: XLineBreakpointType<XBreakpointProperties<*>>) {
    breakpointManager.addBreakpointListener(
      type, listener
    )
  }

  companion object {
    val HIGHLIGHTING_COLOR: Color = Color.getHSBColor(0.74f, 1.0f, 0.97f)
  }
}
