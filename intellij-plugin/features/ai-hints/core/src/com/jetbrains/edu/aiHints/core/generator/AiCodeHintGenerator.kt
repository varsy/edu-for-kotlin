package com.jetbrains.edu.aiHints.core.generator

import com.jetbrains.edu.aiHints.core.connector.HintsServiceConnector
import com.jetbrains.educational.ml.hints.context.CodeHintContext
import com.jetbrains.educational.ml.hints.generator.CodeHintGenerator
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.processors.TaskProcessor

class AiCodeHintGenerator(taskProcessor: TaskProcessor) : CodeHintGenerator(taskProcessor) {
  override suspend fun queryHint(context: CodeHintContext): CodeHint {
    return HintsServiceConnector.getInstance().getCodeHint(context)
  }
}