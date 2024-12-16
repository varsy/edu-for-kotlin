package com.jetbrains.edu.cognifire

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.cognifire.highlighting.prompttocode.PromptToCodeHighlighter
import com.jetbrains.edu.cognifire.parsers.ProdeExpressionParser
import com.jetbrains.edu.cognifire.highlighting.GuardedBlockManager
import com.jetbrains.edu.cognifire.highlighting.HighlighterManager
import com.jetbrains.edu.learning.selectedEditor
import com.intellij.openapi.project.DumbService

class CognifireStartupActivity : ProjectActivity, Disposable {
  private var connection: MessageBusConnection? = null

  override suspend fun execute(project: Project) {
    project.selectedEditor?.virtualFile?.let { addListenersToAllProdeInFile(it, project) }
    connection = project.messageBus.connect()
    connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        super.fileOpened(source, file)
        addListenersToAllProdeInFile(file, project)
      }
    })
  }

  private fun addListenersToAllProdeInFile(file: VirtualFile, project: Project) = DumbService.getInstance(project).runWhenSmart {
    ApplicationManager.getApplication().executeOnPooledThread {
      if (project.isDisposed) return@executeOnPooledThread
      val expressions = runReadAction {
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runReadAction null
        ProdeExpressionParser.getProdeExpressions(psiFile, psiFile.language)
      } ?: return@executeOnPooledThread
      invokeLater {
        if (project.isDisposed) return@invokeLater
        expressions.forEach {
          val function = it.promptExpression.functionSignature
          val id = "${function.name}:${function.functionParameters.size}"
          PromptToCodeHighlighter(project, id).setUpDocumentListener(it.promptExpression, it.codeExpression)
          HighlighterManager.getInstance().highlightAllUncommitedChanges(id, project)
          FileDocumentManager.getInstance().getDocument(file)?.let { document ->
            GuardedBlockManager.getInstance().setReadonlyFragmentModificationHandler(document, it)
          }
        }
      }
    }
  }

  override fun dispose() {
    connection?.disconnect()
  }

}
