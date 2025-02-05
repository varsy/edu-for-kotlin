package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.util.asSafely
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.aiHints.core.feedback.FeedbackLikenessSubmit
import com.jetbrains.edu.aiHints.core.feedback.data.ErrorHintFeedbackInfoData
import com.jetbrains.edu.aiHints.core.feedback.data.ErrorHintFeedbackSystemInfoData
import com.jetbrains.edu.aiHints.core.feedback.dialog.ErrorHintFeedbackDialog
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.Nls

class ErrorHintInlineBanner(
  project: Project,
  task: Task,
  message: @Nls String,
  retryAction: Runnable? = null
) : HintInlineBanner(project, task, message, Status.Error) {
  init {
    if (retryAction != null) {
      addAction(EduAIHintsCoreBundle.message("hints.label.retry")) {
        EduAIFeaturesCounterUsageCollector.hintRetryClicked(task)
        close()
        retryAction.run()
      }
    }
  }

  fun addFeedbackButtons(task: Task, taskFileText: String, errorMessage: String): ErrorHintInlineBanner {
    val project = task.project ?: return this
    val course = project.course.asSafely<EduCourse>() ?: return this
    addLikeDislikeActions {
      FeedbackLikenessSubmit.sendFeedbackData(getLikeness(), ErrorHintFeedbackSystemInfoData(
        CommonFeedbackSystemData.getCurrentData(),
        ErrorHintFeedbackInfoData.create(course, task, taskFileText, errorMessage)
      )
      )
    }
    addCommentAction {
      ErrorHintFeedbackDialog(project, course, task, taskFileText, errorMessage, getLikeness()).show()
    }
    return this
  }
}