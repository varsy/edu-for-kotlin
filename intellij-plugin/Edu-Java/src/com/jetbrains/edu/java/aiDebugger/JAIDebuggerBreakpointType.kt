package com.jetbrains.edu.java.aiDebugger

import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.Icon

class JAIDebuggerBreakpointType : JavaLineBreakpointType("java-line-ai", EduCoreBundle.message("ai.debugger.java.breakpoint.description")) {
  override fun getEnabledIcon(): Icon {
    return EducationalCoreIcons.AIDebugger.BREAKPOINT
  }
}
