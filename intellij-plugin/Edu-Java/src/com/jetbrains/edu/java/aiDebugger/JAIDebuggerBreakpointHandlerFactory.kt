package com.jetbrains.edu.java.aiDebugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaBreakpointHandler
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory

class JAIDebuggerBreakpointHandlerFactory : JavaBreakpointHandlerFactory {
  override fun createHandler(process: DebugProcessImpl): JavaBreakpointHandler =
  	JavaBreakpointHandler(JAIDebuggerBreakpointType::class.java, process)
}
