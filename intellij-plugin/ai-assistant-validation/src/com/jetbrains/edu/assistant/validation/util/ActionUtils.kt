package com.jetbrains.edu.assistant.validation.util

import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.eduAssistant.check.EduAssistantValidationCheckListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun runCheckAction(project: Project) {
  val checkListener = CheckListener.EP_NAME.findExtension(EduAssistantValidationCheckListener::class.java)
                      ?: error("Check listener not found")
  checkListener.clear()

  withContext(Dispatchers.EDT) {
    val dataContext = SimpleDataContext.getProjectContext(project)
    ActionUtil.invokeAction(CheckAction(), dataContext, "", null, null)
  }

  checkListener.wait()
}
