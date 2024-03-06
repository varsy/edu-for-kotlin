package com.jetbrains.edu.learning.actions

import com.intellij.diff.chains.DiffRequestChain
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DocumentContentBase
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.ex.temp.TempFileSystem
import com.jetbrains.edu.learning.isUnitTestMode

fun tryApplyTexts(
  project: Project,
  diffRequestChain: DiffRequestChain,
  fileNames: List<String>,
  actionId: String,
  actionText: String,
): Boolean {
  return try {
    val localDocuments = readLocalDocuments(fileNames)
    check(localDocuments.size == fileNames.size)
    val appliedTexts = diffRequestChain.getTexts(fileNames.size)
    val runnableCommand = { localDocuments.writeTexts(appliedTexts) }
    CommandProcessor.getInstance().executeCommand(project, runnableCommand, actionText, actionId)
    true
  } catch (e: Exception) {
    false
  }
}

fun DiffRequestChain.getTexts(size: Int): List<String> {
  val diffRequestWrappers = List(size) { requests[it] as SimpleDiffRequestChain.DiffRequestProducerWrapper }
  val diffRequests = diffRequestWrappers.map { it.request as SimpleDiffRequest }
  return diffRequests.map { it.contents[1] as DocumentContentBase }.map { it.document.text }
}

fun List<Document>.writeTexts(texts: List<String>): Unit = runWriteAction {
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
  }

  return file?.let {
    FileDocumentManager.getInstance().getDocument(it)
  }
}
