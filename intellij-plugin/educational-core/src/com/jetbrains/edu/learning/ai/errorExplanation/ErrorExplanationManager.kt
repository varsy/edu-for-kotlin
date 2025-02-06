package com.jetbrains.edu.learning.ai.errorExplanation

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.lang.Language
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.edu.learning.checker.StderrAnalyzer
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.ui.EduColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Font
import java.io.File

@Service(Service.Level.PROJECT)
class ErrorExplanationManager(private val project: Project, private val scope: CoroutineScope) {
  fun getErrorExplanation(language: Language, stderr: String) {
    scope.launch {
      withBackgroundProgress(project, "Fetching exception explanation") {
        if (language.id != EduFormatNames.PYTHON) return@withBackgroundProgress
        println("STDERR:\n$stderr\n")
        val stackTrace = getFileAndLineNumber(language, stderr)
        if (stackTrace == null) {
          println("Found no line")
          return@withBackgroundProgress
        }
        val (fileName, lineNumber) = stackTrace
        val vfsFile = VfsUtil.findFileByIoFile(File(fileName), true) ?: return@withBackgroundProgress
        val document = runReadAction { FileDocumentManager.getInstance().getDocument(vfsFile) } ?: return@withBackgroundProgress
        val text = document.text
        val programmingLanguage = project.course?.languageById?.displayName ?: return@withBackgroundProgress
        val result =  ErrorExplanationConnector.getInstance().getErrorExplanation(programmingLanguage, text, stderr)
        val errorExplanation = objectMapper.readValue(result, ErrorExplanation::class.java)
        println("RESULT:\n$result\n")
        withContext(Dispatchers.EDT) {
          openEditor(vfsFile, lineNumber)
          showNotification(project, errorExplanation)
        }
      }
    }
  }

  private var prevRangeHighlighter: Pair<VirtualFile, RangeHighlighter>? = null

  private fun getFileAndLineNumber(language: Language, stderr: String): Pair<String, Int>? {
    val analyzer = StderrAnalyzer.getInstance(language) ?: return null
    val stackTrace = analyzer.getStackTrace(stderr)
    val errorLine = stackTrace.findLast { (fileName, lineNumber) -> isCourseFile(fileName) && lineNumber > 0}
    return errorLine?.let { (fileName, lineNumber) -> Pair(fileName, lineNumber) }
  }

  private fun openEditor(vfsFile: VirtualFile, lineNumber: Int) {
    removePrevHighlighter(project)
    val descriptor = OpenFileDescriptor(project, vfsFile, lineNumber, 0)
    val fileEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true) ?: return
    descriptor.navigateInEditor(project, true)
    fileEditor.markupModel.removeAllHighlighters()
    val attributes = TextAttributes(null, EduColors.aiGetHintHighlighterColor, null, EffectType.BOXED, Font.PLAIN)
    val newRangeHighlighter = fileEditor.markupModel.addLineHighlighter(lineNumber - 1, 0, attributes)
    prevRangeHighlighter = vfsFile to newRangeHighlighter
  }

  private fun removePrevHighlighter(project: Project) {
    val (prevFile, prevRangeHighlighter) = prevRangeHighlighter ?: return
    val descriptor = OpenFileDescriptor(project, prevFile, 0, 0)
    val fileEditor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true) ?: return
    fileEditor.markupModel.removeHighlighter(prevRangeHighlighter)
    prevRangeHighlighter.dispose()
    this.prevRangeHighlighter = null
  }

  private fun showNotification(project: Project, errorExplanation: ErrorExplanation) {
    EduNotificationManager.showInfoNotification(project, "Error Explanation", errorExplanation.explanation)
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

  companion object {
    fun getInstance(project: Project): ErrorExplanationManager = project.service()

    private val objectMapper = jacksonObjectMapper()
  }
}