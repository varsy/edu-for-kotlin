package com.jetbrains.edu.learning.ai.errorExplanation

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.runInBackground
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Service(Service.Level.PROJECT)
class ErrorExplanationManager(private val project: Project, private val scope: CoroutineScope) {
  fun getErrorExplanation(stderr: String) {
    println("STDERR:\n$stderr\n")
    val stackTrace = getFileAndLineNumber(stderr)
    if (stackTrace == null) {
      println("Found no line")
      return
    }
    val (fileName, lineNumber) = stackTrace
    val vfsFile = VfsUtil.findFileByIoFile(File(fileName), true) ?: return
    val document = runReadAction { FileDocumentManager.getInstance().getDocument(vfsFile) } ?: return
    val text = document.text
    val programmingLanguage = project.course?.languageById?.displayName ?: return
    val result = computeUnderProgress(project, "Fetching exception explanation") {
      ErrorExplanationConnector.getInstance ().getErrorExplanation(programmingLanguage, text, stderr)
    }
    println("RESULT:\n$result\n")
    scope.launch {
      withContext(Dispatchers.EDT) {
        openEditor(vfsFile, lineNumber)
      }
    }
  }

  private fun openEditor(vfsFile: VirtualFile, lineNumber: Int) {
    val fileEditor = FileEditorManager.getInstance(project).openFile(vfsFile, true).single()
    val descriptor = OpenFileDescriptor(project, vfsFile, lineNumber - 1, 0)
    descriptor.navigateInEditor(project, true)
  }

  private fun getFileAndLineNumber(stderr: String): Pair<String, Int>? {
    val stackTrace = getStackTrace(stderr)
    val errorLine = stackTrace.findLast { (fileName, lineNumber) -> isCourseFile(fileName) && lineNumber > 0}
    return errorLine?.let { (fileName, lineNumber) -> Pair(fileName, lineNumber) }
  }

  private fun isCourseFile(fileName: String): Boolean {
    return try {
      val file = File(fileName)
      val virtualFile = VfsUtil.findFileByIoFile(file, true) ?: return false
      val taskFile = virtualFile.getTaskFile(project) ?: return false
      if (!taskFile.isVisible) return false
      true
    } catch (e: Exception) {
      false
    }
  }

  private fun getStackTrace(stderr: String): List<StackTraceLine> {
    val regex = Regex("""File "(.+)", line (\d+)""")
    val matches = regex.findAll(stderr).map { it.destructured }.toList()
    return matches.map { (fileName, lineNumber) -> StackTraceLine(fileName, lineNumber.toInt()) }
  }

  private data class StackTraceLine(val fileName: String, val lineNumber: Int)

  companion object {
    fun getInstance(project: Project): ErrorExplanationManager = project.service()
  }
}