package com.jetbrains.edu.learning.aiDebugging.breakpoint

import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.XDebuggerUtil

@Service(Service.Level.PROJECT)
class BreakpointToggleService(private val project: Project) {
  fun toggleLineBreakpoint(language: Language, file: VirtualFile, line: Int) {
    val breakpointType = BreakpointTypeManager.getInstance(language).getBreakPointType()
    val type = XDebuggerUtil.getInstance().findBreakpointType(breakpointType::class.java)
    XDebuggerUtil.getInstance().toggleLineBreakpoint(project, type, file, line)
  }
}
