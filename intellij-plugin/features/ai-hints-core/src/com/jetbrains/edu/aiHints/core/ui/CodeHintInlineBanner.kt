package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.util.asSafely
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.aiHints.core.feedback.FeedbackLikenessSubmit
import com.jetbrains.edu.aiHints.core.feedback.data.CodeHintFeedbackInfoData
import com.jetbrains.edu.aiHints.core.feedback.data.CodeHintFeedbackSystemInfoData
import com.jetbrains.edu.aiHints.core.feedback.dialog.CodeHintFeedbackDialog
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.hint.TextHint
import org.jetbrains.annotations.Nls

class CodeHintInlineBanner(
  project: Project,
  task: Task,
  message: @Nls String,
  private val highlighter: RangeHighlighter? = null
) : HintInlineBanner(project, task, message) {

  override fun removeNotify() {
    super.removeNotify()
    highlighter?.dispose()
  }

  fun addCodeHint(showInCodeAction: () -> Unit): CodeHintInlineBanner {
    addAction(EduAIHintsCoreBundle.message("action.Educational.Hints.GetHint.show.code.text")) {
      EduAIFeaturesCounterUsageCollector.hintShowInCodeClicked(task)
      showInCodeAction()
    }
    return this
  }

  fun addFeedbackLikenessButtons(task: Task, studentSolution: String, textHint: TextHint, codeHint: CodeHint): CodeHintInlineBanner {
    val project = task.project ?: return this
    val course = project.course.asSafely<EduCourse>() ?: return this

    addLikeDislikeActions {
      FeedbackLikenessSubmit.sendFeedbackData(getLikeness(), CodeHintFeedbackSystemInfoData(
        CommonFeedbackSystemData.getCurrentData(),
        CodeHintFeedbackInfoData.create(course, task, studentSolution, textHint, codeHint)
      ))
    }
    return this
  }

  fun addFeedbackCommentButton(task: Task, studentSolution: String, textHint: TextHint, codeHint: CodeHint): CodeHintInlineBanner {
    val project = task.project ?: return this
    val course = project.course.asSafely<EduCourse>() ?: return this

    addCommentAction {
      CodeHintFeedbackDialog(project, course, task, studentSolution, textHint, codeHint, getLikeness()).show()
    }
    return this
  }
}