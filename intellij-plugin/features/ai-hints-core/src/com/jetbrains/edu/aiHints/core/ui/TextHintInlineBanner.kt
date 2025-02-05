package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.feedback.FeedbackLikenessSubmit
import com.jetbrains.edu.aiHints.core.feedback.data.TextHintFeedbackSystemInfoData
import com.jetbrains.edu.aiHints.core.feedback.data.TextHintFeedbackInfoData
import com.jetbrains.edu.aiHints.core.feedback.dialog.TextHintFeedbackDialog
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.hint.TextHint
import org.jetbrains.annotations.Nls

class TextHintInlineBanner(
  project: Project,
  task: Task,
  message: @Nls String,
) : HintInlineBanner(project, task, message) {
  fun addFeedbackLikenessButtons(task: Task, studentSolution: String, textHint: TextHint): TextHintInlineBanner {
    val project = task.project ?: return this
    val course = project.course.asSafely<EduCourse>() ?: return this
    addLikeDislikeActions {
      FeedbackLikenessSubmit.sendFeedbackData(getLikeness(), TextHintFeedbackSystemInfoData(
        CommonFeedbackSystemData.getCurrentData(),
        TextHintFeedbackInfoData.create(course, task, studentSolution, textHint)
      ))
    }
    return this
  }

  fun addFeedbackCommentButton(task: Task, studentSolution: String, textHint: TextHint): TextHintInlineBanner {
    val project = task.project ?: return this
    val course = project.course.asSafely<EduCourse>() ?: return this
    addCommentAction {
      TextHintFeedbackDialog(project, course, task, studentSolution, textHint, getLikeness()).show()
    }
    return this
  }
}
