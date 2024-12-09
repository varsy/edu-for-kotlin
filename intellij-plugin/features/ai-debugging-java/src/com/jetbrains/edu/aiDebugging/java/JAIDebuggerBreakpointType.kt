package com.jetbrains.edu.aiDebugging.java

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import com.jetbrains.edu.aiDebugging.core.ui.AIDebuggingIcons
import javax.swing.Icon

class JAIDebuggerBreakpointType : JavaLineBreakpointType("java-line-ai", EduAIDebuggingCoreBundle.message("ai.debugger.java.breakpoint.description")) {
  // TODO change to real image
  override fun getEnabledIcon(): Icon = AIDebuggingIcons.AIBug
}
