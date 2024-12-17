package com.jetbrains.edu.aiDebugging.jvm

import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.jetbrains.edu.aiDebugging.core.breakpoint.BreakpointTypeManager

@Suppress("UNCHECKED_CAST")
class JvmBreakpointTypeManager : BreakpointTypeManager {
  override fun getBreakPointType(): XLineBreakpointType<XBreakpointProperties<*>> =
    JvmAIDebuggerBreakpointType() as XLineBreakpointType<XBreakpointProperties<*>>
}
