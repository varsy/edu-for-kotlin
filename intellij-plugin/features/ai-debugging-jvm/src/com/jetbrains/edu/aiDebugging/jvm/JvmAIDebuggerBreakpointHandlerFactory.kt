package com.jetbrains.edu.aiDebugging.jvm

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaBreakpointHandler
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory

class JvmAIDebuggerBreakpointHandlerFactory : JavaBreakpointHandlerFactory {
  override fun createHandler(process: DebugProcessImpl): JavaBreakpointHandler =
    JavaBreakpointHandler(JvmAIDebuggerBreakpointType::class.java, process)
}
