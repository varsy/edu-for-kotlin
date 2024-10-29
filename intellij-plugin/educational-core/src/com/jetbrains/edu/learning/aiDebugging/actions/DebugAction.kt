package com.jetbrains.edu.learning.aiDebugging.actions

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.CaretModel
import com.jetbrains.edu.learning.aiDebugging.breakpoint.AIBreakPointService

// TODO temporary class for testing ai breakpoints
class DebugAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: error("Project hasn't been defined")
    val language = Language.findLanguageByID("JAVA") ?: error("Language hasn't been founded") // TODO
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: error("File hasn't been found")
    val line = getLineBreakpointPosition(e) ?: error("Line hasn't been found")
    val aiBreakPointService = project.getService(AIBreakPointService::class.java)
    aiBreakPointService.toggleLineBreakpoint(language, file, line)
  }
}

private fun getLineBreakpointPosition(e: AnActionEvent): Int? {
  val editor = e.getData(CommonDataKeys.EDITOR)
  if (editor == null) return null
  val caretModel: CaretModel = editor.caretModel
  val logicalPosition = caretModel.logicalPosition
  return logicalPosition.line
}
