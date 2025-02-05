package com.jetbrains.edu.aiHints.core.feedback

import com.intellij.platform.feedback.impl.FEEDBACK_REPORT_ID_KEY
import com.intellij.platform.feedback.impl.FeedbackRequestData
import com.intellij.platform.feedback.impl.FeedbackRequestType
import com.intellij.platform.feedback.impl.submitFeedback
import com.jetbrains.edu.ai.translation.ui.LikeBlock.FeedbackLikenessAnswer
import com.jetbrains.edu.aiHints.core.feedback.data.HintFeedbackSystemInfoData
import kotlinx.serialization.json.*

object FeedbackLikenessSubmit {
  const val FEEDBACK_REPORT_ID = "edu_ai_hints_feedback"
  const val SYSTEM_INFO_JSON_KEY = "system_info"
  const val HINTS_LIKENESS_JSON_KEY = "hints_likeness"

  inline fun <reified T> sendFeedbackData(likeness: FeedbackLikenessAnswer, systemInfoData: HintFeedbackSystemInfoData<T>) {
    val feedbackData = FeedbackRequestData(FEEDBACK_REPORT_ID, buildJsonObject {
      put(FEEDBACK_REPORT_ID_KEY, FEEDBACK_REPORT_ID)

      put(HINTS_LIKENESS_JSON_KEY, likeness.result)

      val json = Json { prettyPrint = true }
      put(SYSTEM_INFO_JSON_KEY, json.encodeToJsonElement(systemInfoData))
    })
    submitFeedback(feedbackData, { }, { }, FeedbackRequestType.PRODUCTION_REQUEST)
  }
}
