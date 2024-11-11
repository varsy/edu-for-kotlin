package com.jetbrains.edu.learning.aiDebugging.breakpoint

import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.jetbrains.edu.learning.course
import java.awt.Color

@Service(Service.Level.PROJECT)
class AIBreakPointService(private val project: Project) {

  private val breakpointManager = XDebuggerManager.getInstance(project).breakpointManager
  private val listener = AIBreakpointListener(project)

  fun initialize() {
    val language = Language.findLanguageByID(project.course?.languageId)
    language?.let {
      val breakpointType = BreakpointTypeManager.getInstance(it).getBreakPointType()
      val type = XDebuggerUtil.getInstance().findBreakpointType(breakpointType::class.java)
      breakpointManager.getBreakpoints(type).forEach { breakpoint ->
        listener.highlightBreakpoint(breakpoint)
      }
      addListener(type)
    }
  }


  fun toggleLineBreakpoint(language: Language, file: VirtualFile, line: Int) {
    val breakpointType = BreakpointTypeManager.getInstance(language).getBreakPointType()
    val type = XDebuggerUtil.getInstance().findBreakpointType(breakpointType::class.java)
    breakpointManager.addLineBreakpoint(type, file.url, line, type.createProperties())

  }

  private fun addListener(type: XLineBreakpointType<XBreakpointProperties<*>>) {
    breakpointManager.addBreakpointListener(
      type, listener
    )
  }

  companion object {
    val HIGHLIGHTING_COLOR: Color = Color.getHSBColor(0.75f, 1.0f, 0.98f)
  }
}
