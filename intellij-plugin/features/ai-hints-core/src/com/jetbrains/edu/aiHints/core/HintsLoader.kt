package com.jetbrains.edu.aiHints.core

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.edu.aiHints.core.generator.AiCodeHintGenerator
import com.jetbrains.edu.aiHints.core.generator.AiTextHintGenerator
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.aiHints.core.ui.CodeHintInlineBanner
import com.jetbrains.edu.aiHints.core.ui.ErrorHintInlineBanner
import com.jetbrains.edu.aiHints.core.ui.TextHintInlineBanner
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.selectedTaskFile
import com.jetbrains.edu.learning.ui.EduColors
import com.jetbrains.educational.ml.core.exception.AiAssistantException
import com.jetbrains.educational.ml.hints.assistant.AiHintsAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.awt.Font

@Service(Service.Level.PROJECT)
class HintsLoader(private val project: Project, private val scope: CoroutineScope) {

  fun getHint(task: Task) {
    scope.launch(Dispatchers.Default) {
      if (!mutex.tryLock()) {
        withContext(Dispatchers.EDT) {
          ErrorHintInlineBanner(project, task, EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.already.in.progress")).display()
        }
        return@launch
      }
      try {
        val taskProcessor = TaskProcessorImpl(task)
        val taskFile = taskProcessor.currentTaskFile ?: project.selectedTaskFile ?: error("TaskFile for ${task.name} not found")
        val taskVirtualFile = taskFile.getVirtualFile(project) ?: error("VirtualFile for ${taskFile.name} not found")
        val taskFileText = taskVirtualFile.getTextFromTaskTextFile() ?: error("TaskFile text for ${taskFile.name} not found")

        val hintsAssistant = AiHintsAssistant.getAssistant(taskProcessor, AiCodeHintGenerator(taskProcessor), AiTextHintGenerator())
        val hint = withBackgroundProgress(project, EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.progress.text"), cancellable = true) {
          withContext(Dispatchers.IO) {
            hintsAssistant.getHint(taskProcessor.getSubmissionTextRepresentation() ?: "")
          }
        }.getOrElse {
          withContext(Dispatchers.EDT) {
            val errorMessage = AiAssistantException.get(it).message
            ErrorHintInlineBanner(project, task, errorMessage) { getHint(task) }
              .addFeedbackLikenessButtons(task, taskFileText, errorMessage)
              .addFeedbackCommentButton(task, taskFileText, errorMessage)
              .display()
          }
          return@launch
        }

        val codeHint = hint.codeHint
        if (codeHint != null) {
          withContext(Dispatchers.EDT) {
            val highlighter = highlightFirstCodeDiffPositionOrNull(project, taskVirtualFile, taskFileText, codeHint.code)
            CodeHintInlineBanner(project, task, hint.textHint.text, highlighter)
              .addCodeHint { showInCodeAction(project, taskVirtualFile, taskFileText, codeHint.code) }
              .addFeedbackLikenessButtons(task, taskFileText, hint.textHint, codeHint)
              .addFeedbackCommentButton(task, taskFileText, hint.textHint, codeHint)
              .display()
          }
          return@launch
        }
        withContext(Dispatchers.EDT) {
          TextHintInlineBanner(project, task, hint.textHint.text)
            .addFeedbackLikenessButtons(task, taskFileText, hint.textHint)
            .addFeedbackCommentButton(task, taskFileText, hint.textHint)
            .display()
        }
      }
      finally {
        mutex.unlock()
      }
    }
  }

  private val mutex = Mutex()

  @RequiresEdt
  private fun highlightFirstCodeDiffPositionOrNull(
    project: Project,
    taskVirtualFile: VirtualFile,
    taskFileText: String,
    codeHint: String
  ): RangeHighlighter? {
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
    val currentVirtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
    if (currentVirtualFile != taskVirtualFile) return null

    val startLine = ComparisonManager.getInstance().compareLines(
      taskFileText, codeHint, ComparisonPolicy.DEFAULT, DumbProgressIndicator.INSTANCE
    ).firstOrNull()?.startLine1 ?: return null
    if (startLine >= taskFileText.lines().size) return null

    val attributes = TextAttributes(null, EduColors.aiGetHintHighlighterColor, null, EffectType.BOXED, Font.PLAIN)
    return editor.markupModel.addLineHighlighter(startLine, 0, attributes)
  }

  @RequiresEdt
  private fun showInCodeAction(project: Project, taskVirtualFile: VirtualFile, taskFileText: String, codeHint: String) {
    val diffRequestChain = SimpleDiffRequestChain(
      SimpleDiffRequest(
        EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.diff.title"),
        DiffContentFactory.getInstance().create(taskFileText, taskVirtualFile.fileType),
        DiffContentFactory.getInstance().create(codeHint, taskVirtualFile.fileType),
        EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.current.solution"),
        EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.solution.after.changes")
      )
    )
    diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, listOf(taskVirtualFile.path))
    diffRequestChain.putUserData(ApplyCodeAction.GET_HINT_DIFF, true)
    DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
  }

  companion object {
    fun getInstance(project: Project): HintsLoader = project.service()

    fun isRunning(project: Project): Boolean = getInstance(project).mutex.isLocked
  }
}