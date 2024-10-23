package com.jetbrains.edu.ai.hints.ui

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.JBColor
import com.jetbrains.edu.ai.hints.action.GetHint.Companion.NEXT_STEP_HINT_DIFF_FLAG
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls
import java.awt.Font

object HintsBannerManager {

  suspend fun showCodeHintBanner(project: Project, task: Task, taskFile: TaskFile, message: @Nls String, codeHint: String) {
    val highlighter = highlightFirstCodeDiffPositionOrNull(project, taskFile, codeHint)
    val hintBanner = HintInlineBanner(message).apply {
      addAction(EduAIBundle.message("action.Educational.Hints.GetHint.show.code.text")) {
        showInCodeAction(project, taskFile, codeHint)
      }
      setCloseAction {
        highlighter?.dispose()
        TaskToolWindowView.getInstance(project).removeInlineBannerFromCheckPanel(this)
      }
    }
    show(project, task, hintBanner)
  }

  suspend fun showTextHintBanner(project: Project, task: Task, message: @Nls String) = show(
    project,
    task,
    HintInlineBanner(message).apply {
      setCloseAction {
        TaskToolWindowView.getInstance(project).removeInlineBannerFromCheckPanel(this)
      }
    }
  )

  private suspend fun show(project: Project, task: Task, hintsBanner: HintInlineBanner) = withContext(Dispatchers.EDT) {
    val taskToolWindow = TaskToolWindowView.getInstance(project)
    taskToolWindow.updateCheckPanel(task)
    taskToolWindow.addInlineBannerToCheckPanel(hintsBanner)
  }

  // TODO: Refactor
  private fun showInCodeAction(project: Project, taskFile: TaskFile, codeHint: String) {
    val virtualFile = taskFile.getVirtualFile(project) ?: error("VirtualFile for ${taskFile.name} not found")
    val solutionContent = DiffContentFactory.getInstance().create(VfsUtil.loadText(virtualFile), virtualFile.fileType)
    val solutionAfterChangesContent = DiffContentFactory.getInstance().create(codeHint, virtualFile.fileType)
    val request = SimpleDiffRequest(
      EduAIBundle.message("action.Educational.Hints.GetHint.diff.title"),
      solutionContent,
      solutionAfterChangesContent,
      EduAIBundle.message("action.Educational.Hints.GetHint.current.solution"),
      EduAIBundle.message("action.Educational.Hints.GetHint.solution.after.changes")
    )
    val diffRequestChain = SimpleDiffRequestChain(request)
    diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf(virtualFile.path))
    diffRequestChain.putUserData(NEXT_STEP_HINT_DIFF_FLAG, true)
    DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
  }

  /**
   * Highlights the first code difference position between the student's code in the task file and a given code hint.
   *
   * @return The range highlighter indicating the first code difference position, or null
   * if virtualFile or editor is null or
   * if the focus is on another file or
   * if no differences are found.
   */
  // TODO: Refactor
  private fun highlightFirstCodeDiffPositionOrNull(project: Project, taskFile: TaskFile, codeHint: String): RangeHighlighter? {
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
    val virtualFile = taskFile.getVirtualFile(project) ?: return null
    val currentFile = FileDocumentManager.getInstance().getFile(editor.document)
    if (currentFile != virtualFile) {
      return null
    }
    val studentText = VfsUtil.loadText(virtualFile)
    val fragments = ComparisonManager.getInstance().compareLines(
      studentText, codeHint, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE
    )
    return fragments.firstOrNull()?.startLine1?.let { line ->
      val attributes =
        TextAttributes(null, JBColor(HIGHLIGHTER_COLOR_RGB, HIGHLIGHTER_COLOR_DARK_RGB), null, EffectType.BOXED, Font.PLAIN)
      if (line < studentText.lines().size) {
        editor.markupModel.addLineHighlighter(line, 0, attributes)
      }
      else {
        null
      }
    }
  }

  private const val HIGHLIGHTER_COLOR_RGB: Int = 0xEFE5FF

  private const val HIGHLIGHTER_COLOR_DARK_RGB: Int = 0x433358
}