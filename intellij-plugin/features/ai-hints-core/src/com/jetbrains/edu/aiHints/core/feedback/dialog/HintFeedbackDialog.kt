package com.jetbrains.edu.aiHints.core.feedback.dialog

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialog
import com.intellij.platform.feedback.dialog.SystemDataJsonSerializable
import com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
import com.intellij.platform.feedback.dialog.uiBlocks.TextAreaBlock
import com.intellij.ui.dsl.builder.Panel
import com.jetbrains.edu.ai.translation.ui.LikeBlock
import com.jetbrains.edu.ai.translation.ui.LikeBlock.FeedbackLikenessAnswer
import com.jetbrains.edu.aiHints.core.feedback.data.HintFeedbackCommonInfoData
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager

abstract class HintFeedbackDialog<T : SystemDataJsonSerializable>(
  private val project: Project,
  defaultLikeness: FeedbackLikenessAnswer = FeedbackLikenessAnswer.NO_ANSWER
) : BlockBasedFeedbackDialog<T>(project, false) {
  override val myFeedbackReportId: String = "edu_ai_hints_feedback"

  override val myTitle: String
    get() = EduAIHintsCoreBundle.message("hints.feedback.dialog.title")

  override val myBlocks: List<FeedbackBlock> = listOf(
    LikeBlock(
      EduAIHintsCoreBundle.message("hints.feedback.like.label"),
      "hints_likeness",
      defaultLikeness
    ),
    TextAreaBlock("", "hints_experience")
      .setPlaceholder(EduCoreBundle.message("ui.feedback.dialog.textarea.optional.label"))
  )

  override fun showThanksNotification() {
    EduNotificationManager.showInfoNotification(
      project = project,
      title = EduAIHintsCoreBundle.message("hints.feedback.notification.title"),
      content = EduAIHintsCoreBundle.message("hints.feedback.notification.text")
    )
  }

  protected fun Panel.commonFeedbackData(mySystemInfoData: HintFeedbackCommonInfoData) {
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.id")) {
      label(mySystemInfoData.courseId.toString())
    }
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.update.version")) {
      label(mySystemInfoData.courseUpdateVersion.toString())
    }
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.course.name")) {
      label(mySystemInfoData.courseName)
    }
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.id")) {
      label(mySystemInfoData.taskId.toString())
    }
    row(EduCoreBundle.message("ui.feedback.dialog.system.info.task.name")) {
      label(mySystemInfoData.taskName)
    }
    row(EduAIHintsCoreBundle.message("hints.feedback.label.student.solution")) {
      label(mySystemInfoData.studentSolution)
    }
  }
}