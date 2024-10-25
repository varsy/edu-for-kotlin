package com.jetbrains.edu.ai.hints.generator

import com.jetbrains.edu.ai.hints.connector.HintsServiceConnector
import com.jetbrains.educational.ml.hints.context.CodeHintContext
import com.jetbrains.educational.ml.hints.generator.CodeHintGenerator
import com.jetbrains.educational.ml.hints.hint.CodeHint
import com.jetbrains.educational.ml.hints.processors.TaskProcessor

class AiCodeHintGenerator(taskProcessor: TaskProcessor) : CodeHintGenerator(taskProcessor) {
  override suspend fun queryHint(context: CodeHintContext): CodeHint {
    return HintsServiceConnector.getInstance().getCodeHint(context)
  }
}