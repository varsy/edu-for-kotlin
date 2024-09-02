package com.jetbrains.edu.learning.actions

import com.intellij.diff.chains.DiffRequestChain
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DocumentContentBase
import com.intellij.diff.editor.ChainDiffVirtualFile
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.closeLastActiveFileEditor
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.isUnitTestMode

abstract class ApplyCodeActionBase : DumbAwareAction(), CustomComponentAction {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isStudentProject() && e.isUserDataPresented() && !e.isNextStepHintDiff()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (!getConfirmationFromDialog(project)) return

    val diffRequestChain = e.getDiffRequestChain() ?: return showFailedNotification(project)
    val fileNames = diffRequestChain.getUserData(VIRTUAL_FILE_PATH_LIST).takeIf { !it.isNullOrEmpty() }
                    ?: return showFailedNotification(project)

    try {
      val localDocuments = readLocalDocuments(fileNames)
      check(localDocuments.size == fileNames.size)
      val appliedTexts = diffRequestChain.getTexts(fileNames.size)
      val runnableCommand = { localDocuments.writeTexts(appliedTexts) }
      CommandProcessor.getInstance().executeCommand(project, runnableCommand, actionId, templatePresentation.text)
    } catch (_: Exception) {
      showFailedNotification(project)
      return
    }
    project.invokeLater {
      project.closeLastActiveFileEditor(e)
    }
    showSuccessfulNotification(project)
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  protected abstract fun showSuccessfulNotification(project: Project)

  protected abstract fun showFailedNotification(project: Project)

  protected abstract fun getConfirmationFromDialog(project: Project): Boolean

  protected abstract val actionId: String

  private fun DiffRequestChain.getTexts(size: Int): List<String> {
    val diffRequestWrappers = List(size) { requests[it] as SimpleDiffRequestChain.DiffRequestProducerWrapper }
    val diffRequests = diffRequestWrappers.map { it.request as SimpleDiffRequest }
    return diffRequests.map { it.contents[1] as DocumentContentBase }.map { it.document.text }
  }

  private fun List<Document>.writeTexts(texts: List<String>): Unit = runWriteAction {
    zip(texts).forEach { (document, text) ->
      document.setText(text)
      FileDocumentManager.getInstance().saveDocument(document)
    }
  }

  private fun readLocalDocuments(fileNames: List<String>): List<Document> = runReadAction {
    fileNames.mapNotNull { findLocalDocument(it) }
  }

  private fun findLocalDocument(fileName: String): Document? {
    val file = if (!isUnitTestMode) {
      LocalFileSystem.getInstance().findFileByPath(fileName)
    }
    else {
      TempFileSystem.getInstance().findFileByPath(fileName)
    } ?: return null

    return FileDocumentManager.getInstance().getDocument(file)
  }

  private fun AnActionEvent.isUserDataPresented() = getDiffRequestChain()?.getUserData(VIRTUAL_FILE_PATH_LIST) != null

  companion object {
    val VIRTUAL_FILE_PATH_LIST: Key<List<String>> = Key.create("virtualFilePathList")

    fun AnActionEvent.getDiffRequestChain(): DiffRequestChain? {
      val chainDiffVirtualFile = getData(CommonDataKeys.VIRTUAL_FILE) as? ChainDiffVirtualFile
      return chainDiffVirtualFile?.chain
    }

    fun AnActionEvent.isNextStepHintDiff(): Boolean =
      getDiffRequestChain()?.getUserData(NextStepHintAction.NEXT_STEP_HINT_DIFF_FLAG) == true
  }
}
