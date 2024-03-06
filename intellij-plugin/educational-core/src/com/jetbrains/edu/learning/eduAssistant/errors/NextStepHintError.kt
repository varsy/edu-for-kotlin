package com.jetbrains.edu.learning.eduAssistant.errors

import com.jetbrains.edu.learning.messages.EduCoreBundle

enum class NextStepHintError(val errorMessage: String) {
  UnknownError("${EduCoreBundle.message("action.Educational.NextStepHint.error.common")} ${EduCoreBundle.message("action.Educational.NextStepHint.error.unknown")}"),
  UnlockedError("${EduCoreBundle.message("action.Educational.NextStepHint.error.common")} ${EduCoreBundle.message("action.Educational.NextStepHint.error.unlocked")}")
}
