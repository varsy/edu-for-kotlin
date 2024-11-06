package com.jetbrains.edu.learning.aiDebugging.session

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.intellij.platform.ide.progress.withModalProgress
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.educational.ml.ai.debugger.prompt.core.FixCodeForTestAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class LaunchDebugSessionService(private val project: Project, private val coroutineScope: CoroutineScope) {

  fun startSession(description: String, selectedTaskFile: TaskFile, testDescription: String) {
    coroutineScope.launch {
      withModalProgress(project, EduCoreBundle.message("action.Educational.AiDebuggingNotification.modal.session")) {
        FixCodeForTestAssistant.getCodeFix(description, selectedTaskFile.contents.textualRepresentation, testDescription)
      }.onSuccess { fixes ->
        runReadAction {
          val virtualFile =
            selectedTaskFile.getVirtualFile(project)
            ?: error("There are no virtual file for the selected task : `$selectedTaskFile`")
          val document = virtualFile.document
          fixes.forEach {
            val offset = virtualFile.readText().indexOf(it.wrongCode)
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
