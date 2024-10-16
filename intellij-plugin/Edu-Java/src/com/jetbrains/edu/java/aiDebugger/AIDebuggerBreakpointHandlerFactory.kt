package com.jetbrains.edu.java.aiDebugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaBreakpointHandler
import com.intellij.debugger.engine.JavaBreakpointHandlerFactory

class AIDebuggerBreakpointHandlerFactory : JavaBreakpointHandlerFactory {
  override fun createHandler(process: DebugProcessImpl): JavaBreakpointHandler {
    return AIDebuggerBreakpointHandler(process)
  }
}

class AIDebuggerBreakpointHandler(process: DebugProcessImpl) : JavaBreakpointHandler(AIDebuggerBreakpoint::class.java, process)
