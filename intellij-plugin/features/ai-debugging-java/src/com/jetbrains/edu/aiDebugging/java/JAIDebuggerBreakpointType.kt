package com.jetbrains.edu.aiDebugging.java

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.jetbrains.edu.aiDebugging.core.breakpoint.BreakPointTypeHelper
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import javax.swing.Icon

class JAIDebuggerBreakpointType : JavaLineBreakpointType("java-line-ai", EduAIDebuggingCoreBundle.message("ai.debugger.java.breakpoint.description")) {
  // TODO change to real image
  override fun getEnabledIcon(): Icon = BreakPointTypeHelper.getEnabledIcon()
}
