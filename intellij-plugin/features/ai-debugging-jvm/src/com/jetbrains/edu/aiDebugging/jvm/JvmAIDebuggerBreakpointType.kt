package com.jetbrains.edu.aiDebugging.jvm

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.jetbrains.edu.aiDebugging.core.messages.EduAIDebuggingCoreBundle
import com.jetbrains.edu.aiDebugging.core.ui.AIDebuggingIcons
import javax.swing.Icon

class JvmAIDebuggerBreakpointType : JavaLineBreakpointType("jvm-line-ai", EduAIDebuggingCoreBundle.message("ai.debugger.jvm.breakpoint.description")) {
  // TODO change to real image
  override fun getEnabledIcon(): Icon = AIDebuggingIcons.AIBug
}
