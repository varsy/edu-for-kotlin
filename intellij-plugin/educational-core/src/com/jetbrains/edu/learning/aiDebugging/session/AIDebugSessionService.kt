package com.jetbrains.edu.learning.aiDebugging.session

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.ide.progress.withModalProgress
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.ai.debugger.prompt.core.FixCodeForTestAssistant
import com.jetbrains.educational.ml.ai.debugger.prompt.prompt.entities.description.TaskDescription
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class AIDebugSessionService(private val project: Project, private val coroutineScope: CoroutineScope) {

  fun runDebuggingSession(description: TaskDescription, virtualFiles: List<VirtualFile>, testDescription: String) {
    coroutineScope.launch {
      withModalProgress(project, EduCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session")) {
        FixCodeForTestAssistant.getCodeFix(
          description,
          virtualFiles.first().readText(),
          testDescription
        ) // TODO change to several files and grab text in a proper way
      }.onSuccess { fixes ->
        runReadAction {
          val virtualFile = virtualFiles.first() // TODO change to several files
          val document = virtualFile.document
          fixes.forEach {
            val offset = virtualFile.readText().indexOf(it.wrongCode)
            require(offset >= 0)
            { "There are no offset in the file for the current wrong code: `${it.wrongCode}`" }
            val line = document.getLineNumber(offset) + 1
            // TODO toggle breakpoint
          }
        }
      }.onFailure {
        EduNotificationManager.showErrorNotification(
          project,
          content = EduCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session.fail")
        )
      }
    }
  }
}
