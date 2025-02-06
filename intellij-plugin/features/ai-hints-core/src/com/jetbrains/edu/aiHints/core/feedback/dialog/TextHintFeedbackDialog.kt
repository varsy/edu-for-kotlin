package com.jetbrains.edu.aiHints.core.feedback.dialog

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.jetbrains.edu.ai.translation.ui.LikeBlock.FeedbackLikenessAnswer
import com.jetbrains.edu.aiHints.core.feedback.data.TextHintFeedbackInfoData
import com.jetbrains.edu.aiHints.core.feedback.data.TextHintFeedbackSystemInfoData
import com.jetbrains.edu.aiHints.core.log.Logger
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.ml.hints.hint.TextHint

class TextHintFeedbackDialog(
  private val project: Project,
  private val course: Course,
  private val task: Task,
  private val studentSolution: String,
  private val textHint: TextHint,
  defaultLikeness: FeedbackLikenessAnswer = FeedbackLikenessAnswer.NO_ANSWER
) : HintFeedbackDialog<TextHintFeedbackSystemInfoData>(project, defaultLikeness) {

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showFeedbackSystemInfoDialog(project, mySystemInfoData.commonSystemInfo) {
      commonFeedbackData(mySystemInfoData.hintFeedbackInfo.hintFeedbackInfoData)
      row(EduAIHintsCoreBundle.message("hints.feedback.label.text.hint")) {
        label(mySystemInfoData.hintFeedbackInfo.textHint.text)
      }
    }
  }

  override val mySystemInfoData: TextHintFeedbackSystemInfoData by lazy {
    TextHintFeedbackSystemInfoData(
      CommonFeedbackSystemData.getCurrentData(),
      TextHintFeedbackInfoData.create(course, task, studentSolution, textHint)
    )
  }

  init {
    init()
  }

  override fun sendFeedbackData() {
    super.sendFeedbackData()
    Logger.aiHintsLogger.info(
      """|| Course id: ${task.course.id} | Lesson id: ${task.lesson.id} | Task id: ${task.id}
         || Feedback: ${collectDataToJsonObject()}
         || Type: text hint
      """.trimMargin()
    )
  }
}