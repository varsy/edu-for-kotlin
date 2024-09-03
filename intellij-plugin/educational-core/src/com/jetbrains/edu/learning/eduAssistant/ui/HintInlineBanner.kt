package com.jetbrains.edu.learning.eduAssistant.ui

import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.openapi.editor.markup.EffectType
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.InlineBanner
import com.intellij.ui.JBColor
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.Nls
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.border.CompoundBorder

class HintInlineBanner(project: Project, message: @Nls String, action: () -> Unit = {}) : InlineBanner(message) {
  init {
    setIcon(EducationalCoreIcons.Actions.AiAssistant)
    isOpaque = false
    border = createBorder()
    background = JBColor(BACKGROUND_COLOR_RGB, BACKGROUND_COLOR_DARK_RGB)
    addAction(EduCoreBundle.message("action.Educational.NextStepHint.show.code.text")) {
      action()
    }
    setCloseAction {
      TaskToolWindowView.getInstance(project).removeInlineBannerFromCheckPanel(this)
    }
  }

  private fun createBorder(): CompoundBorder = BorderFactory.createCompoundBorder(
    RoundedLineBorder(
      JBColor(BORDER_COLOR_RGB, BORDER_COLOR_DARK_RGB), NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get()
    ), JBUI.Borders.empty(BORDER_OFFSET)
  )

  companion object {
    private const val BACKGROUND_COLOR_RGB: Int = 0xFAF5FF
    private const val BACKGROUND_COLOR_DARK_RGB: Int = 0x2F2936
    private const val BORDER_COLOR_RGB: Int = 0xDCCBFB
    private const val BORDER_COLOR_DARK_RGB: Int = 0x8150BE
    private const val BORDER_OFFSET: Int = 10
    private const val HIGHLIGHTER_COLOR_RGB: Int = 0xEFE5FF
    private const val HIGHLIGHTER_COLOR_DARK_RGB: Int = 0x433358

    /**
     * Highlights the first code difference position between the student's code in the task file and a given code hint.
     *
     * @return The range highlighter indicating the first code difference position, or null
     * if virtualFile or editor is null or
     * if the focus is on another file or
     * if no differences are found.
     */
    fun highlightFirstCodeDiffPositionOrNull(project: Project, taskFile: TaskFile, codeHint: String): RangeHighlighter? {
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
  }
}