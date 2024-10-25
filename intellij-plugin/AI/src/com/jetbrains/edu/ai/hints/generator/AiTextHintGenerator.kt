package com.jetbrains.edu.ai.hints.generator

import com.jetbrains.edu.ai.hints.connector.HintsServiceConnector
import com.jetbrains.educational.ml.hints.context.TextHintContext
import com.jetbrains.educational.ml.hints.generator.TextHintGenerator
import com.jetbrains.educational.ml.hints.hint.TextHint

class AiTextHintGenerator : TextHintGenerator() {
  override suspend fun queryHint(context: TextHintContext): TextHint {
    return HintsServiceConnector.getInstance().getTextHint(context)
  }
}