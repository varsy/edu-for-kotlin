package com.jetbrains.edu.java.aiDebugger

import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.jetbrains.edu.learning.aiDebugging.breakpoint.BreakpointTypeManager

@Suppress("UNCHECKED_CAST")
class BreakpointTypeManagerJava : BreakpointTypeManager {
  override fun getBreakPointType(): XLineBreakpointType<XBreakpointProperties<*>> {
    return AIDebuggerBreakpoint() as XLineBreakpointType<XBreakpointProperties<*>>
  }
}
